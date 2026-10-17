package io.github.areiljan.cmrsign.dss

import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.PadesSigner
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.certificate
import io.github.areiljan.cmrsign.keyPair
import io.github.areiljan.cmrsign.unsignedPdf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PadesSignerRoundTripTest {
    @Test
    fun `a pades-signed pdf passes DSS and derives NA`() {
        val keyPair = keyPair()
        val signingCertificate = certificate(keyPair)
        val signed = PadesSigner(keyPair.private, listOf(signingCertificate))
            .sign(unsignedPdf(), "Test Signer")

        val verdict = DssPadesValidator(listOf(signingCertificate))
            .validate(DocumentToValidate(signed, MediaType.PDF))

        assertEquals(Indication.TOTAL_PASSED, verdict.indication)
        assertEquals(Qualification.NA, verdict.qualification)
        assertEquals(signingCertificate.subjectX500Principal.name, verdict.signer.subjectName)
    }
}
