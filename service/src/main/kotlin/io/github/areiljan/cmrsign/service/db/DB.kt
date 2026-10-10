package io.github.areiljan.cmrsign.service.db

import klite.Config
import klite.Server
import klite.info
import klite.logger
import klite.jdbc.DBModule
import klite.jdbc.DBMigrator
import klite.jdbc.PooledDataSource

fun Server.initDB() {
    logger().info("Migrating " + Config["DB_URL"])
    use(DBMigrator())
    use(DBModule(PooledDataSource()))
}
