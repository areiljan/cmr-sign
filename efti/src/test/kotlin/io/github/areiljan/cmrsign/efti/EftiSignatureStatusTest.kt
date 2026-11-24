package io.github.areiljan.cmrsign.efti

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.Indication.*
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.Qualification.*
import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.Verdict
import org.junit.jupiter.api.Test

class EftiSignatureStatusTest {
    @Test
    fun `a passing personal signature attests ADES`() {
        for (qualification in listOf(QESIG, QES, ADESIG, ADES))
            expect(status(TOTAL_PASSED, qualification).signatureType).toEqual(EftiSignatureType.ADES)
    }

    @Test
    fun `a passing seal attests ADESEAL`() {
        for (qualification in listOf(QESEAL, ADESEAL))
            expect(status(TOTAL_PASSED, qualification).signatureType).toEqual(EftiSignatureType.ADESEAL)
    }

    @Test
    fun `a passing signature that is not advanced does not attest`() {
        expect(status(TOTAL_PASSED, NA).atLeastAdvanced).toEqual(false)
    }

    @Test
    fun `a failed or indeterminate signature never attests`() {
        for (indication in listOf(TOTAL_FAILED, INDETERMINATE))
            for (qualification in Qualification.entries)
                expect(status(indication, qualification).atLeastAdvanced).toEqual(false)
    }

    private fun status(indication: Indication, qualification: Qualification) = EftiSignatureStatus.from(
        EftiDataElement.EFTI65_SENDER,
        Verdict.from(indication, null, qualification, SignerIdentity("CN=Sender"), "report"),
        "report",
    )
}
