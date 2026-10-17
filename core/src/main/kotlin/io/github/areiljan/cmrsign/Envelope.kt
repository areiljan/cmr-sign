package io.github.areiljan.cmrsign

data class Envelope(
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val state: EnvelopeState = EnvelopeState.CREATED,
    val id: Id<Envelope> = Id('E'),
)

enum class EnvelopeState {
    CREATED,
    SESSION_STARTED,
    IDENTITY_PROVEN,
}

fun EnvelopeState.startSession(): EnvelopeState = when (this) {
    EnvelopeState.CREATED -> EnvelopeState.SESSION_STARTED
    else -> error("cannot start a session from $this")
}

fun EnvelopeState.confirmIdentity(): EnvelopeState = when (this) {
    EnvelopeState.SESSION_STARTED -> EnvelopeState.IDENTITY_PROVEN
    else -> error("cannot confirm identity from $this")
}
