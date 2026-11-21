package io.github.areiljan.cmrsign

// what level of qualification does the signature have per eIDAS
enum class Qualification {
    QESIG,
    QESEAL,
    QES, // qualified, but DSS cannot tell signature from seal
    ADESIG,
    ADESEAL,
    ADES, // advanced, but DSS cannot tell signature from seal
    NA; // not applicable, indeterminate or not advanced
}
