package io.github.areiljan.cmrsign.demo

import io.github.areiljan.cmrsign.SigningProvider
import io.github.areiljan.cmrsign.Validator
import io.github.areiljan.cmrsign.dss.DssPadesValidator
import io.github.areiljan.cmrsign.demo.db.initDB
import io.github.areiljan.cmrsign.demo.envelopes.EnvelopeRoutes
import io.github.areiljan.cmrsign.demo.reference.ReferenceSigningProvider
import io.github.areiljan.cmrsign.demo.security.PlatformKeyGuard
import klite.AssetsHandler
import klite.Config
import klite.Server
import klite.annotations.annotated
import klite.isProd
import klite.jdbc.RequestTransactionHandler
import klite.json.JsonBody
import klite.register
import java.nio.file.Path
import kotlin.io.path.exists

fun main() {
    if (!Config.isProd) Config.useEnvFile()
    val platformKey = Config.required("PLATFORM_API_KEY")
    val assetsPath = Path.of("ui/build").takeIf { it.exists() } ?: Path.of("ui/public")

    Server().apply {
        val signingProvider = ReferenceSigningProvider()
        register<SigningProvider>(signingProvider)
        register<Validator>(DssPadesValidator(listOf(signingProvider.certificate)))
        initDB()
        use<RequestTransactionHandler>()
        use<JsonBody>()

        assets("/", AssetsHandler(assetsPath, useIndexForUnknownPaths = true))

        context("/health") { get { "ok" } }
        context("/api") {
            useOnly<JsonBody>()
            before(PlatformKeyGuard(platformKey))
            annotated<EnvelopeRoutes>("/envelopes")
        }

        start()
    }
}
