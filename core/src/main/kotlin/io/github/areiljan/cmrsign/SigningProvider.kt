package io.github.areiljan.cmrsign

interface SigningProvider {
    fun startSession(signer: String): String

    fun completeSession(session: String): SignerIdentity
}
