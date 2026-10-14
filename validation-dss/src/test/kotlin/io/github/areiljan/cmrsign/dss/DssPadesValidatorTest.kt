package io.github.areiljan.cmrsign.dss

import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Qualification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class DssPadesValidatorTest {
    @Test
    fun `a valid signed pdf passes and derives NA`() {
        val verdict = DssPadesValidator(listOf(signerCertificate()))
            .validate(DocumentToValidate(validSignedPdf(), MediaType.PDF))

        assertEquals(Indication.TOTAL_PASSED, verdict.indication)
        assertEquals(Qualification.NA, verdict.qualification)
    }

    private fun signerCertificate(): X509Certificate =
        resource("/signer-cert.pem").use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

    private fun validSignedPdf(): ByteArray = resource("/valid-signed.pdf").use { it.readBytes() }

    private fun resource(name: String) =
        requireNotNull(javaClass.getResourceAsStream(name)) { "missing test resource: $name" }
}
