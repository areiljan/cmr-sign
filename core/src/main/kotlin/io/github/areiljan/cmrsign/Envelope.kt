package io.github.areiljan.cmrsign

data class Envelope(
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val state: EnvelopeState = EnvelopeState.CREATED,
    val id: Id<Envelope> = Id('E'),
)

sealed interface EnvelopeState {
    data object CREATED : EnvelopeState
}
