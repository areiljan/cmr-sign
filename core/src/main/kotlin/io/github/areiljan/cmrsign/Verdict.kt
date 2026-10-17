package io.github.areiljan.cmrsign

class Verdict private constructor(
    val indication: Indication,
    val subIndication: String?,
    val qualification: Qualification,
    val signer: SignerIdentity,
) {
    companion object {
        fun from(
            indication: Indication,
            subIndication: String?, // the reason when indication is TOTAL_PASSED
            qualification: Qualification,
            signer: SignerIdentity,
        ): Verdict = Verdict(indication, subIndication, qualification, signer)
    }
}
