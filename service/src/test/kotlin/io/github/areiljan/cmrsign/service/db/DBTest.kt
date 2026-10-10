package io.github.areiljan.cmrsign.service.db

import klite.Config
import klite.jdbc.DBMigrator

abstract class DBTest : klite.jdbc.DBTest() {
    companion object {
        init {
            Config["DB_URL"] += "_test"
            DBMigrator().migrate()
        }
    }
}
