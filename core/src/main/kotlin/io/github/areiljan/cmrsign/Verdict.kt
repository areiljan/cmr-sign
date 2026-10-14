package io.github.areiljan.cmrsign

class Verdict private constructor(
    val indication: Indication,
    val qualification: Qualification,
) {
    companion object {
        fun from(indication: Indication, qualification: Qualification): Verdict =
            Verdict(indication, qualification)
    }
}
