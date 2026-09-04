package com.sultonuzdev.netspeed.data.database


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sultonuzdev.netspeed.data.database.dao.UsageDao
import com.sultonuzdev.netspeed.data.database.entities.UsageEntity

@Database(
    entities = [UsageEntity::class],
    version = 4,
    exportSchema = false
)
abstract class NetSpeedDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        /**
         * Added the speed-test history table.
         *
         * A real migration rather than a destructive one: by this version the usage table holds
         * the user's own history, which a destructive fallback would silently erase on upgrade.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `speed_test_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `downloadBps` REAL NOT NULL,
                        `uploadBps` REAL NOT NULL,
                        `pingMillis` INTEGER NOT NULL,
                        `jitterMillis` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `networkType` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Drops it again: speed-test results are no longer kept. Installs that never saw v3 skip
         * straight here, so the drop is written defensively.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `speed_test_table`")
            }
        }
    }
}
