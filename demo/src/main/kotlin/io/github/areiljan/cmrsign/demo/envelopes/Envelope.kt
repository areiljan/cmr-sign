package io.github.areiljan.cmrsign.demo.envelopes

import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.MediaType
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.demo.db.Entity
import io.github.areiljan.cmrsign.efti.EftiDataElement
import io.github.areiljan.cmrsign.efti.EftiSignatureType
import java.time.Instant

data class Envelope(
    val format: SignatureFormat,
    val signerClass: SignerClass,
    val eftiRole: EftiDataElement,
    val signerName: String? = null,
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

data class ValidationReport(
    val envelopeId: Id<DomainEnvelope>,
    val documentVersion: Int,
    val content: String,
    val eftiSignatureType: EftiSignatureType?,
    override val id: Id<ValidationReport> = Id('R'),
): Entity<ValidationReport>

data class SigningLink(
    val envelopeId: Id<DomainEnvelope>,
    val tokenHash: Sha256,
    val otpHash: Sha256,
    val expiresAt: Instant,
    val failedAttempts: Int = 0,
    val verifiedAt: Instant? = null,
    val usedAt: Instant? = null,
    override val id: Id<SigningLink> = Id('L'),
): Entity<SigningLink>
