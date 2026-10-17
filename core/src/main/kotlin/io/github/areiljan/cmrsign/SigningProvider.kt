package io.github.areiljan.cmrsign

interface SigningProvider {
    fun startSession(signer: String): String

    fun completeSession(session: String): SignerIdentity

    fun sign(session: String, dataToSign: ByteArray): ByteArray
}
