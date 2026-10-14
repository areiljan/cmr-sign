package io.github.areiljan.cmrsign

class Verdict private constructor(
    val indication: Indication,
    val qualification: Qualification,
    val signer: SignerIdentity,
) {
    companion object {
        fun from(
            indication: Indication,
            qualification: Qualification,
            signer: SignerIdentity,
        ): Verdict = Verdict(indication, qualification, signer)
    }
}
