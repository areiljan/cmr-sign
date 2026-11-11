package io.github.areiljan.cmrsign.cades

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.pades.PadesValidator
import org.junit.jupiter.api.Test
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class DssCmsValidatorTest {
    @Test
    fun `a valid signed pdf passes and derives NA`() {
        val verdict = PadesValidator(DssCmsValidator(listOf(signerCertificate())))
            .validate(DocumentToValidate(validSignedPdf(), MediaType.PDF))

        expect(verdict.indication).toEqual(Indication.TOTAL_PASSED)
        expect(verdict.qualification).toEqual(Qualification.NA)
    }

    @Test
    fun `reports who signed`() {
        val certificate = signerCertificate()

        val verdict = PadesValidator(DssCmsValidator(listOf(certificate)))
            .validate(DocumentToValidate(validSignedPdf(), MediaType.PDF))

        expect(verdict.signer.subjectName).toEqual(certificate.subjectX500Principal.name)
    }

    private fun signerCertificate(): X509Certificate =
        resource("/signer-cert.pem").use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

    private fun validSignedPdf(): ByteArray = resource("/valid-signed.pdf").use { it.readBytes() }

    private fun resource(name: String) =
        requireNotNull(javaClass.getResourceAsStream(name)) { "missing test resource: $name" }
}
