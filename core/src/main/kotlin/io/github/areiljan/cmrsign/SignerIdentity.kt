package io.github.areiljan.cmrsign

import java.security.cert.X509Certificate

class SignerIdentity(
    val subjectName: String,
    val certificateChain: List<X509Certificate> = emptyList(),
    val proofingMethod: String? = null,
)
