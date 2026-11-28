package io.github.areiljan.cmrsign.cades

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.Verdict
import io.github.areiljan.cmrsign.efti.EftiDataElement
import io.github.areiljan.cmrsign.efti.EftiSignatureStatus
import io.github.areiljan.cmrsign.pades.PadesValidator
import org.apache.pdfbox.pdmodel.PDDocument
import org.bouncycastle.asn1.ASN1InputStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File

@EnabledIfEnvironmentVariable(named = "PADES_PROBE_FILE", matches = ".+")
class ExternalPadesProbeTest {
    private val pdf = File(System.getenv("PADES_PROBE_FILE")).readBytes()
    private val validator = DssLotlCmsValidator()

    @Test
    fun `an externally signed PAdES gets a positive eFTI status from the EU trusted lists`() {
        val signatures = PDDocument.load(pdf).use { document ->
            document.signatureDictionaries.map { Triple(it.subFilter, it.byteRange.let { r -> r[2] + r[3] }, it) }
        }
        println("probe: ${validator.trustedCertificateCount} trusted-list certificates")
        signatures.forEach { (subFilter, coveredBytes, _) -> println("probe: $subFilter covers $coveredBytes of ${pdf.size} bytes") }

        val (_, _, signature) = signatures.first { it.first != "ETSI.RFC3161" }
        val cms = ASN1InputStream(signature.getContents(pdf)).use { it.readObject().encoded }
        val cmsVerdict = validator.validate(cms, signature.getSignedContent(pdf))
        report("CMS", cmsVerdict)

        val pipelineVerdict = PadesValidator(validator).validate(pdf)
        report("pipeline", pipelineVerdict)

        expect(cmsVerdict.indication).toEqual(Indication.TOTAL_PASSED)
        expect(EftiSignatureStatus.from(EftiDataElement.EFTI65_SENDER, cmsVerdict, "probe").atLeastAdvanced).toEqual(true)
    }

    private fun report(label: String, verdict: Verdict) {
        File("build/pades-probe-$label-report.xml").writeText(verdict.report)
        println("probe $label: ${masked(verdict.signer.subjectName)} -> ${verdict.indication} ${verdict.subIndication} ${verdict.qualification}")
    }

    private fun masked(subject: String) = subject.replace(Regex("\\d{6,}")) { it.value.first() + "*".repeat(it.value.length - 2) + it.value.last() }
}
