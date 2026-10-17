package io.github.areiljan.cmrsign.dss

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.PadesSigner
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.certificate
import io.github.areiljan.cmrsign.keyPair
import io.github.areiljan.cmrsign.unsignedPdf
import org.junit.jupiter.api.Test

class PadesSignerRoundTripTest {
    val keyPair = keyPair()
    val signingCertificate = certificate(keyPair)
    val signed = PadesSigner(keyPair.private, listOf(signingCertificate))
        .sign(unsignedPdf(), "Test Signer")

    @Test
    fun `a pades-signed pdf passes DSS and derives NA`() {
        val verdict = DssPadesValidator(listOf(signingCertificate))
            .validate(DocumentToValidate(signed, MediaType.PDF))

        expect(verdict.indication).toEqual(Indication.TOTAL_PASSED)
        expect(verdict.subIndication).toEqual(null)
        expect(verdict.qualification).toEqual(Qualification.NA)
        expect(verdict.signer.subjectName).toEqual(signingCertificate.subjectX500Principal.name)
    }

    @Test
    fun `a tampered signed pdf is rejected`() {
        val tampered = tamperTitle(signed)
        val verdict = DssPadesValidator(listOf(signingCertificate))
            .validate(DocumentToValidate(tampered, MediaType.PDF))

        expect(verdict.indication).toEqual(Indication.TOTAL_FAILED)
        expect(verdict.subIndication).toEqual("HASH_FAILURE")
    }

    private fun tamperTitle(pdf: ByteArray): ByteArray {
        val text = String(pdf, Charsets.ISO_8859_1)
        val title = text.indexOf("/Title (")
        require(title >= 0) { "missing title metadata" }
        val offset = title + "/Title (".length + 1
        return pdf.copyOf().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
    }
}
