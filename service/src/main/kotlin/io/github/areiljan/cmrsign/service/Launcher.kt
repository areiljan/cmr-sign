package io.github.areiljan.cmrsign.service

import io.github.areiljan.cmrsign.service.db.initDB
import io.github.areiljan.cmrsign.service.envelopes.EnvelopeRoutes
import io.github.areiljan.cmrsign.service.security.PlatformKeyGuard
import klite.Config
import klite.Server
import klite.annotations.annotated
import klite.isProd
import klite.jdbc.RequestTransactionHandler
import klite.json.JsonBody

fun main() {
    if (!Config.isProd) Config.useEnvFile()
    val platformKey = Config.required("PLATFORM_API_KEY")

    Server().apply {
        initDB()
        use<RequestTransactionHandler>()
        use<JsonBody>()

        context("/health") { get { "ok" } }
        context("/api") {
            useOnly<JsonBody>()
            before(PlatformKeyGuard(platformKey))
            annotated<EnvelopeRoutes>("/envelopes")
        }

        start()
    }
}
