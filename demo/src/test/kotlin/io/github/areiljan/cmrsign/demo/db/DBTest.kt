package io.github.areiljan.cmrsign.demo.db

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
