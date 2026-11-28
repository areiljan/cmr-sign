package io.github.areiljan.cmrsign

data class Envelope(
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val state: EnvelopeState = EnvelopeState.CREATED,
    val id: Id<Envelope> = Id('E'),
)

enum class EnvelopeState {
    CREATED,
    AUTHORIZATION_PENDING,
    IDENTITY_PROVEN,
    SIGNED,
    VALIDATED,
}

fun EnvelopeState.startSession(status: SessionStatus): EnvelopeState = when {
    this != EnvelopeState.CREATED -> error("cannot start a session from $this")
    status == SessionStatus.AUTHORIZED -> EnvelopeState.IDENTITY_PROVEN
    status == SessionStatus.PENDING -> EnvelopeState.AUTHORIZATION_PENDING
    else -> error("cannot start a session with status $status")
}

fun EnvelopeState.authorize(status: SessionStatus): EnvelopeState = when {
    this != EnvelopeState.AUTHORIZATION_PENDING -> error("cannot authorize from $this")
    status == SessionStatus.AUTHORIZED -> EnvelopeState.IDENTITY_PROVEN
    else -> error("provider session is $status")
}

fun EnvelopeState.sign(): EnvelopeState = when (this) {
    EnvelopeState.IDENTITY_PROVEN -> EnvelopeState.SIGNED
    else -> error("cannot sign from $this")
}

fun EnvelopeState.validate(): EnvelopeState = when (this) {
    EnvelopeState.SIGNED -> EnvelopeState.VALIDATED
    else -> error("cannot validate from $this")
}
