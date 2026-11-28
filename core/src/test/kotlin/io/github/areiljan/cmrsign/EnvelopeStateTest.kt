package io.github.areiljan.cmrsign

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test

class EnvelopeStateTest {
    @Test
    fun `an authorized session proves identity at once`() {
        expect(EnvelopeState.CREATED.startSession(SessionStatus.AUTHORIZED)).toEqual(EnvelopeState.IDENTITY_PROVEN)
    }

    @Test
    fun `a pending session waits for authorization`() {
        val pending = EnvelopeState.CREATED.startSession(SessionStatus.PENDING)

        expect(pending).toEqual(EnvelopeState.AUTHORIZATION_PENDING)
        expect(pending.authorize(SessionStatus.AUTHORIZED)).toEqual(EnvelopeState.IDENTITY_PROVEN)
    }

    @Test
    fun `cannot sign before authorization`() {
        expect { EnvelopeState.AUTHORIZATION_PENDING.sign() }.toThrow<IllegalStateException>()
    }

    @Test
    fun `a failed or expired session does not prove identity`() {
        expect { EnvelopeState.CREATED.startSession(SessionStatus.FAILED) }.toThrow<IllegalStateException>()
        expect { EnvelopeState.AUTHORIZATION_PENDING.authorize(SessionStatus.EXPIRED) }.toThrow<IllegalStateException>()
    }

    @Test
    fun `cannot authorize twice`() {
        expect { EnvelopeState.IDENTITY_PROVEN.authorize(SessionStatus.AUTHORIZED) }.toThrow<IllegalStateException>()
    }
}
