package io.github.areiljan.cmrsign.dss

import eu.europa.esig.dss.enumerations.Indication as DssIndication
import eu.europa.esig.dss.enumerations.SignatureQualification as DssQualification
import eu.europa.esig.dss.model.InMemoryDocument
import eu.europa.esig.dss.model.x509.CertificateToken
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource
import eu.europa.esig.dss.validation.SignedDocumentValidator
import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.Validator
import io.github.areiljan.cmrsign.Verdict
import java.security.cert.X509Certificate

class DssPadesValidator(
    private val trustedCertificates: List<X509Certificate>,
) : Validator {
    override fun validate(document: DocumentToValidate): Verdict {
        val validator = SignedDocumentValidator.fromDocument(InMemoryDocument(document.bytes))
        val trusted = CommonTrustedCertificateSource().apply {
            trustedCertificates.forEach { addCertificate(CertificateToken(it)) }
        }
        validator.setCertificateVerifier(
            CommonCertificateVerifier().apply { setTrustedCertSources(trusted) },
        )

        val simple = validator.validateDocument().simpleReport
        val id = requireNotNull(simple.firstSignatureId) { "no signature found in the document" }

        return Verdict.from(
            indication = indicationOf(simple.getIndication(id)),
            qualification = qualificationOf(simple.getSignatureQualification(id)),
        )
    }

    private fun indicationOf(dss: DssIndication): Indication = when (dss) {
        DssIndication.TOTAL_PASSED -> Indication.TOTAL_PASSED
        else -> throw UnsupportedOperationException("unsupported DSS indication: $dss")
    }

    private fun qualificationOf(dss: DssQualification): Qualification = when (dss) {
        DssQualification.NA -> Qualification.NA
        else -> throw UnsupportedOperationException("unsupported DSS qualification: $dss")
    }
}
