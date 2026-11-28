package io.github.areiljan.cmrsign

import java.time.Instant

/**
 * Seam between the signing pipeline and the party that holds the key.
 *
 * A session starts [SessionStatus.PENDING] when the provider needs the signer to authorize
 * it (for example with a one-time password) and [SessionStatus.AUTHORIZED] when it does not.
 * [completeSession] and [sign] are only allowed on an authorized session.
 */
interface SigningProvider {
    fun startSession(signer: String): ProviderSession

    fun session(session: String): ProviderSession

    /** Hands the signer's activation code to the provider. The code is never returned or logged. */
    fun authorize(session: String, activationCode: String): ProviderSession =
        throw UnsupportedOperationException("this provider does not take activation codes")

    fun completeSession(session: String): SignerIdentity

    fun sign(session: String, dataToSign: ByteArray): ByteArray
}

data class ProviderSession @JvmOverloads constructor(
    val id: String,
    val status: SessionStatus,
    val activation: Activation = Activation.NONE,
    val expiresAt: Instant? = null,
)

enum class SessionStatus { PENDING, AUTHORIZED, FAILED, EXPIRED }

/** What the signer has to supply before a [SessionStatus.PENDING] session can be authorized. */
enum class Activation { NONE, OTP }
