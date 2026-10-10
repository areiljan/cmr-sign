package io.github.areiljan.cmrsign.service

import klite.Config
import klite.Server
import klite.isProd
import klite.jdbc.DBModule
import klite.jdbc.DBMigrator
import klite.jdbc.PooledDataSource
import klite.jdbc.RequestTransactionHandler
import klite.json.JsonBody

fun main() {
    if (!Config.isProd) Config.useEnvFile()
    Config.required("PLATFORM_API_KEY")

    Server().apply {
        use(DBMigrator())
        use(DBModule(PooledDataSource()))
        use<RequestTransactionHandler>()
        use<JsonBody>()

        context("/health") { get { "ok" } }

        start()
    }
}
