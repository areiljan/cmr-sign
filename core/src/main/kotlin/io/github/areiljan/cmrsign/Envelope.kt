package io.github.areiljan.cmrsign

data class Envelope(
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val state: EnvelopeState = EnvelopeState.CREATED,
    val id: Id<Envelope> = Id('E'),
)

enum class EnvelopeState {
    CREATED,
    IDENTITY_PROVEN,
}

fun EnvelopeState.startSession(): EnvelopeState = when (this) {
    EnvelopeState.CREATED -> EnvelopeState.IDENTITY_PROVEN
    else -> error("cannot start a session from $this")
}
