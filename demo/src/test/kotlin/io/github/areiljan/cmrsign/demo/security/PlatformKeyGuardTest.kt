package io.github.areiljan.cmrsign.demo.security

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.mockk.every
import io.mockk.mockk
import klite.HttpExchange
import klite.UnauthorizedException
import org.junit.jupiter.api.Test

class PlatformKeyGuardTest {

    private fun exchangeWithKey(key: String?): HttpExchange =
        mockk<HttpExchange>().also { every { it.header("X-Api-Key") } returns key }

    @Test
    fun `accepts the expected key`() {
        PlatformKeyGuard("secret").before(exchangeWithKey("secret"))
    }

    @Test
    fun `rejects a missing key`() {
        expect { PlatformKeyGuard("secret").before(exchangeWithKey(null)) }.toThrow<UnauthorizedException>()
    }

    @Test
    fun `rejects a wrong key`() {
        expect { PlatformKeyGuard("secret").before(exchangeWithKey("guess")) }.toThrow<UnauthorizedException>()
    }

    @Test
    fun `compares in constant time regardless of length`() {
        expect(PlatformKeyGuard.constantTimeEquals("secret", "secret")).toEqual(true)
        expect(PlatformKeyGuard.constantTimeEquals("secret", "guessed-longer")).toEqual(false)
    }
}
