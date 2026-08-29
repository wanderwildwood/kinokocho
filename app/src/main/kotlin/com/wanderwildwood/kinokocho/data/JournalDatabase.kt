package com.wanderwildwood.kinokocho.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Observation::class, ObservationCharacter::class, ObservationPhoto::class],
    version = 3,
    exportSchema = true,
)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        private const val NAME = "journal.db"

        /**
         * v1 to v2: observations gain `kept`.
         *
         * Everything already in a journal was put there by the old app, which had no
         * way to hold an unclaimed find — so every existing row is one the reader kept,
         * and they all migrate to kept = 1. Defaulting them to 0 would empty somebody's
         * journal, which is the one thing this database must never do.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE observations ADD COLUMN kept INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE observations SET kept = 1")
            }
        }

        /**
         * v2 to v3: observations gain `identified_as`.
         *
         * Empty for everything already written down, which is true: nobody could record
         * what a find turned out to be, because there was nowhere to record it.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE observations ADD COLUMN identified_as TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var instance: JournalDatabase? = null

        fun get(context: Context): JournalDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): JournalDatabase =
            Room.databaseBuilder(context, JournalDatabase::class.java, NAME)
                // No fallbackToDestructiveMigration, ever. This database holds notes that
                // cannot be taken again - the mushroom is gone and the season is over. A
                // missing migration must fail loudly in a build, not quietly wipe a journal.
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // Room declares the foreign keys but SQLite does not enforce them
                        // unless asked, per connection. Without this, deleting an
                        // observation silently orphans its characters and photographs.
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                .build()
    }
}
