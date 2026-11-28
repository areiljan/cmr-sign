package io.github.areiljan.cmrsign.demo.envelopes

import io.github.areiljan.cmrsign.Activation
import io.github.areiljan.cmrsign.DocumentToValidate
import io.github.areiljan.cmrsign.Envelope as DomainEnvelope
import io.github.areiljan.cmrsign.EnvelopeState
import io.github.areiljan.cmrsign.Id
import io.github.areiljan.cmrsign.SessionStatus
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignerIdentity
import io.github.areiljan.cmrsign.SigningProvider
import io.github.areiljan.cmrsign.Validator
import io.github.areiljan.cmrsign.Verdict
import io.github.areiljan.cmrsign.authorize
import io.github.areiljan.cmrsign.efti.EftiSignatureStatus
import io.github.areiljan.cmrsign.pades.PadesSigner
import io.github.areiljan.cmrsign.sign
import io.github.areiljan.cmrsign.startSession
import io.github.areiljan.cmrsign.validate
import klite.NotFoundException

class SigningService(
    private val envelopes: EnvelopeRepository,
    private val documents: DocumentRepository,
    private val reports: ValidationReportRepository,
    private val signingProvider: SigningProvider,
    private val validator: Validator,
) {
    fun start(envelope: Envelope, proofing: String? = null): SessionStarted {
        val document = original(envelope.id)
        val session = signingProvider.startSession(envelope.signerName ?: envelope.id.value)
        val state = envelope.state.startSession(session.status)
        val identity = if (session.status == SessionStatus.AUTHORIZED) boundIdentity(envelope, session.id) else null
        val updated = envelope.copy(
            state = state,
            providerSessionId = session.id,
            signerSubject = identity?.subjectName,
            proofingMethod = identity?.let { proofingOf(it, proofing) },
        )
        envelopes.save(updated)
        return SessionStarted(updated.id, updated.state, session.id, session.activation, updated.signerSubject, updated.proofingMethod, document.version, document.sha256)
    }

    fun authorize(envelope: Envelope, activationCode: String, proofing: String? = null): SessionStarted {
        val sessionId = requireNotNull(envelope.providerSessionId) { "session not started" }
        check(envelope.state == EnvelopeState.AUTHORIZATION_PENDING) { "envelope is ${envelope.state}, not waiting for authorization" }
        val session = signingProvider.authorize(sessionId, activationCode)
        val state = envelope.state.authorize(session.status)
        val identity = boundIdentity(envelope, sessionId)
        val updated = envelope.copy(state = state, signerSubject = identity.subjectName, proofingMethod = proofingOf(identity, proofing))
        envelopes.save(updated)
        val document = original(envelope.id)
        return SessionStarted(updated.id, updated.state, sessionId, session.activation, updated.signerSubject, updated.proofingMethod, document.version, document.sha256)
    }

    fun sign(envelope: Envelope, documentSha256: Sha256): Signed {
        val sessionId = requireNotNull(envelope.providerSessionId) { "session not started" }
        val document = original(envelope.id)
        require(document.sha256 == documentSha256) { "document hash does not match" }
        val signedState = envelope.state.sign()
        val identity = boundIdentity(envelope, sessionId)
        val signed = PadesSigner.forProvider(signingProvider, sessionId, identity).sign(document.content, identity.subjectName)
        val verdict = validator.validate(DocumentToValidate(signed, document.contentType))
        val version = document.version + 1
        val sha256 = Sha256.of(signed)
        documents.save(Document(envelope.id, version, document.contentType, sha256, signed))
        val reportId = Id<ValidationReport>('R')
        val efti = EftiSignatureStatus.from(envelope.eftiRole, verdict, reportId.value)
        reports.save(ValidationReport(envelope.id, version, verdict.report, efti.signatureType, reportId))
        val updated = envelope.copy(state = signedState.validate())
        envelopes.save(updated)
        return Signed(updated.id, updated.state, version, sha256, verdict, efti)
    }

    fun original(id: Id<DomainEnvelope>): Document =
        documents.by(Document::version to 1, Document::envelopeId to id) ?: throw NotFoundException("no document for $id")

    private fun boundIdentity(envelope: Envelope, sessionId: String): SignerIdentity {
        val identity = signingProvider.completeSession(sessionId)
        envelope.signerName?.let {
            check(identity.isBoundTo(it)) { "the provider certificate does not name the expected signer" }
        }
        return identity
    }

    private fun proofingOf(identity: SignerIdentity, platformProofing: String?) =
        listOfNotNull(platformProofing, identity.proofingMethod).joinToString("+")
}

data class SessionStarted(
    val id: Id<DomainEnvelope>,
    val state: EnvelopeState,
    val providerSessionId: String,
    val activation: Activation,
    val signerSubject: String?,
    val proofingMethod: String?,
    val documentVersion: Int,
    val documentSha256: Sha256,
)

data class Signed(
    val id: Id<DomainEnvelope>,
    val state: EnvelopeState,
    val documentVersion: Int,
    val sha256: Sha256,
    val verdict: Verdict,
    val efti: EftiSignatureStatus,
)
