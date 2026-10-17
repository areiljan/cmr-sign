package io.github.areiljan.cmrsign

import java.security.Signature
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TestSigningProvider : SigningProvider {
    private val keyPair = keyPair()
    val certificate = certificate(keyPair)

    private val sessions = ConcurrentHashMap.newKeySet<String>()

    override fun startSession(signer: String): String {
        require(signer.isNotBlank()) { "signer must not be blank" }
        return UUID.randomUUID().toString().also { sessions.add(it) }
    }

    override fun completeSession(session: String): SignerIdentity {
        require(sessions.contains(session)) { "unknown session: $session" }
        return SignerIdentity(
            subjectName = certificate.subjectX500Principal.name,
            certificateChain = listOf(certificate),
            proofingMethod = "test-provider",
        )
    }

    override fun sign(session: String, dataToSign: ByteArray): ByteArray {
        require(sessions.contains(session)) { "unknown session: $session" }
        return Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(dataToSign)
            sign()
        }
    }
}
