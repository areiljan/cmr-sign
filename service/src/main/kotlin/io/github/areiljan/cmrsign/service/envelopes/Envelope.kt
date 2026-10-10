package io.github.areiljan.cmrsign.service.envelopes

import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.service.db.Entity
import io.github.areiljan.cmrsign.service.db.Id

data class Envelope(
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val state: State = State.CREATED,
    override val id: Id<Envelope> = Id('E'),
): Entity<Envelope> {
    enum class State { CREATED }
}

data class Document(
    val envelopeId: Id<Envelope>,
    val version: Int,
    val contentType: MediaType,
    val sha256: Sha256,
    val content: ByteArray,
    override val id: Id<Document> = Id('D'),
): Entity<Document>
