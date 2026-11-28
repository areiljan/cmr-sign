package io.github.areiljan.cmrsign.cades

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import org.junit.jupiter.api.Test
import java.time.Instant

class TotpTest {
    private val rfc6238Secret = "12345678901234567890".toByteArray()

    @Test
    fun `matches the RFC 6238 SHA-1 test vectors`() {
        expect(totp(rfc6238Secret, Instant.ofEpochSecond(59), digits = 8)).toEqual("94287082")
        expect(totp(rfc6238Secret, Instant.ofEpochSecond(1111111109), digits = 8)).toEqual("07081804")
        expect(totp(rfc6238Secret, Instant.ofEpochSecond(20000000000), digits = 8)).toEqual("65353130")
    }
}
