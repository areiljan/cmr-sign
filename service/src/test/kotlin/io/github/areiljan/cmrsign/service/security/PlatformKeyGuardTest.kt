package io.github.areiljan.cmrsign.service.security

import io.mockk.every
import io.mockk.mockk
import klite.HttpExchange
import klite.UnauthorizedException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
        assertThrows(UnauthorizedException::class.java) {
            PlatformKeyGuard("secret").before(exchangeWithKey(null))
        }
    }

    @Test
    fun `rejects a wrong key`() {
        assertThrows(UnauthorizedException::class.java) {
            PlatformKeyGuard("secret").before(exchangeWithKey("guess"))
        }
    }

    @Test
    fun `compares in constant time regardless of length`() {
        assertTrue(PlatformKeyGuard.constantTimeEquals("secret", "secret"))
        assertFalse(PlatformKeyGuard.constantTimeEquals("secret", "guessed-longer"))
    }
}
