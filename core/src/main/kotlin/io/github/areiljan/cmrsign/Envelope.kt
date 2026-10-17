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
    SIGNED,
    VALIDATED,
}

fun EnvelopeState.startSession(): EnvelopeState = when (this) {
    EnvelopeState.CREATED -> EnvelopeState.IDENTITY_PROVEN
    else -> error("cannot start a session from $this")
}

fun EnvelopeState.sign(): EnvelopeState = when (this) {
    EnvelopeState.IDENTITY_PROVEN -> EnvelopeState.SIGNED
    else -> error("cannot sign from $this")
}

fun EnvelopeState.validate(): EnvelopeState = when (this) {
    EnvelopeState.SIGNED -> EnvelopeState.VALIDATED
    else -> error("cannot validate from $this")
}
