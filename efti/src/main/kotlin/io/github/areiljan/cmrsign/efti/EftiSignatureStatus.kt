package io.github.areiljan.cmrsign.efti

import io.github.areiljan.cmrsign.Indication
import io.github.areiljan.cmrsign.Qualification
import io.github.areiljan.cmrsign.Verdict

enum class EftiDataElement { EFTI65_SENDER, EFTI115_CARRIER, EFTI84_CONSIGNEE }

enum class EftiSignatureType { ADES, ADESEAL }

class EftiSignatureStatus private constructor(
    val dataElement: EftiDataElement,
    val signatureType: EftiSignatureType?,
    val validationReportId: String,
) {
    val atLeastAdvanced: Boolean get() = signatureType != null

    companion object {
        @JvmStatic
        fun from(dataElement: EftiDataElement, verdict: Verdict, validationReportId: String) = EftiSignatureStatus(
            dataElement,
            if (verdict.indication == Indication.TOTAL_PASSED) signatureTypeOf(verdict.qualification) else null,
            validationReportId,
        )

        private fun signatureTypeOf(qualification: Qualification) = when (qualification) {
            Qualification.QESIG, Qualification.QES, Qualification.ADESIG, Qualification.ADES -> EftiSignatureType.ADES
            Qualification.QESEAL, Qualification.ADESEAL -> EftiSignatureType.ADESEAL
            Qualification.NA -> null
        }
    }
}
