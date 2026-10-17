package io.github.areiljan.cmrsign.demo.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.demo.db.Entity

data class Envelope(
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val state: EnvelopeState = EnvelopeState.CREATED,
    val providerSessionId: String? = null,
    val signerSubject: String? = null,
    val proofingMethod: String? = null,
    override val id: Id<DomainEnvelope> = Id('E'),
): Entity<DomainEnvelope>

fun Envelope.toDomain() = DomainEnvelope(format, signerClass, state, id)

data class Document(
    val envelopeId: Id<DomainEnvelope>,
    val version: Int,
    val contentType: MediaType,
    val sha256: Sha256,
    val content: ByteArray,
    override val id: Id<Document> = Id('D'),
): Entity<Document>
