package io.github.areiljan.cmrsign.service.envelopes

import ch.tutteli.atrium.api.fluent.en_GB.toContain
import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import io.github.areiljan.cmrsign.SignatureFormat
import io.github.areiljan.cmrsign.SignerClass
import io.github.areiljan.cmrsign.service.db.DBTest
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class EnvelopeRepositoryTest : DBTest() {
    private val repository = EnvelopeRepository(db)

    @Test
    fun `save and load`() {
        val envelope = Envelope(SignatureFormat.PADES, SignerClass.REGISTERED)
        repository.save(envelope)

        expect(repository.get(envelope.id)).toEqual(envelope)
        expect(repository.list()).toContain(envelope)
    }
}
