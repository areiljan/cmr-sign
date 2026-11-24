package io.github.areiljan.cmrsign.efti

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.Verdict
import org.junit.jupiter.api.Test

class EftiSignatureStatusTest {
    @Test
    fun `a passing advanced or qualified signature attests its type`() {
        val expected = mapOf(
            Qualification.QESIG to EftiSignatureType.ADES,
            Qualification.QES to EftiSignatureType.ADES,
            Qualification.ADESIG to EftiSignatureType.ADES,
            Qualification.ADES to EftiSignatureType.ADES,
            Qualification.QESEAL to EftiSignatureType.ADESEAL,
            Qualification.ADESEAL to EftiSignatureType.ADESEAL,
        )

        val actual = expected.keys.associateWith { status(Indication.TOTAL_PASSED, it) }

        expect(actual.mapValues { it.value.signatureType }).toEqual(expected)
        expect(actual.values.map { it.atLeastAdvanced }.toSet()).toEqual(setOf(true))
    }

    @Test
    fun `a passing signature that is not advanced does not attest`() {
        val status = status(Indication.TOTAL_PASSED, Qualification.NA)

        expect(status.atLeastAdvanced).toEqual(false)
        expect(status.signatureType).toEqual(null)
    }

    @Test
    fun `a failed or indeterminate signature never attests, whatever its qualification`() {
        val statuses = listOf(Indication.TOTAL_FAILED, Indication.INDETERMINATE).flatMap { indication ->
            Qualification.entries.map { status(indication, it) }
        }

        expect(statuses.map { it.atLeastAdvanced }.toSet()).toEqual(setOf(false))
    }

    @Test
    fun `keeps the role and the validation report it is based on`() {
        val status = EftiSignatureStatus.from(
            EftiDataElement.EFTI84_CONSIGNEE,
            Verdict.from(Indication.TOTAL_PASSED, null, Qualification.QESIG, SignerIdentity("CN=Consignee")),
            "report-1",
        )

        expect(status.dataElement).toEqual(EftiDataElement.EFTI84_CONSIGNEE)
        expect(status.validationReportId).toEqual("report-1")
    }

    private fun status(indication: Indication, qualification: Qualification) = EftiSignatureStatus.from(
        EftiDataElement.EFTI65_SENDER,
        Verdict.from(indication, null, qualification, SignerIdentity("CN=Sender")),
        "report",
    )
}
