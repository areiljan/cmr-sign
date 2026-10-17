package io.github.areiljan.cmrsign

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ReferenceSigningProvider : SigningProvider {
    private val sessions = ConcurrentHashMap.newKeySet<String>()

    override fun startSession(signer: String): String {
        require(signer.isNotBlank()) { "signer must not be blank" }
        return UUID.randomUUID().toString().also { sessions.add(it) }
    }

    override fun completeSession(session: String): SignerIdentity {
        require(sessions.remove(session)) { "unknown or already completed session: $session" }
        return SignerIdentity(
            subjectName = "Reference Signer",
            proofingMethod = "reference-provider",
        )
    }
}
