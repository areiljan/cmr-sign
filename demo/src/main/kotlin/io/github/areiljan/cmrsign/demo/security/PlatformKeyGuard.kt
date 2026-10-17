package io.github.areiljan.cmrsign.demo.security

import klite.Before
import klite.HttpExchange
import klite.UnauthorizedException
import java.security.MessageDigest

class PlatformKeyGuard(private val expected: String): Before {
    override fun before(exchange: HttpExchange) {
        val provided = exchange.header("X-Api-Key")
        if (provided == null || !constantTimeEquals(provided, expected)) throw UnauthorizedException("invalid platform key")
    }

    companion object {
        fun constantTimeEquals(provided: String, expected: String) =
            MessageDigest.isEqual(provided.toByteArray(), expected.toByteArray())
    }
}
