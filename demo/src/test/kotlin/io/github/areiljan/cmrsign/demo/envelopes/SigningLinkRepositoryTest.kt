package io.github.areiljan.cmrsign.demo.envelopes

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.github.areiljan.cmrsign.Sha256
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.demo.db.DBTest
import io.github.areiljan.cmrsign.efti.EftiDataElement
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit.MICROS

class SigningLinkRepositoryTest : DBTest() {
    private val envelopes = EnvelopeRepository(db)
    private val repository = SigningLinkRepository(db)

    @Test
    fun `save and find by token hash`() {
        val envelope = Envelope(SignatureFormat.PADES, SignerClass.AD_HOC, EftiDataElement.EFTI84_CONSIGNEE, signerName = "Mari Maasikas")
        envelopes.save(envelope)
        val link = SigningLink(envelope.id, Sha256.of("token".toByteArray()), Sha256.of("123456".toByteArray()), Instant.now().truncatedTo(MICROS))
        repository.save(link)

        expect(repository.by(SigningLink::tokenHash to link.tokenHash)).toEqual(link)
        expect(envelopes.get(envelope.id).signerName).toEqual("Mari Maasikas")

        val used = link.copy(failedAttempts = 1, verifiedAt = link.expiresAt, usedAt = link.expiresAt)
        repository.save(used)
        expect(repository.get(link.id)).toEqual(used)
    }
}
