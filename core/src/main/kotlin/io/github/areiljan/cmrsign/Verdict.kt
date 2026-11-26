package io.github.areiljan.cmrsign

class Verdict private constructor(
    val indication: Indication,
    val subIndication: String?,
    val qualification: Qualification,
    val signer: SignerIdentity,
    val report: String, // ETSI TS 119 102-2 validation report XML
) {
    companion object {
        @JvmStatic
        fun from(
            indication: Indication,
            subIndication: String?, // the reason when indication is not TOTAL_PASSED
            qualification: Qualification,
            signer: SignerIdentity,
            report: String,
        ): Verdict = Verdict(indication, subIndication, qualification, signer, report)
    }
}
