package com.wanderwildwood.kinokocho.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Observation::class, ObservationCharacter::class, ObservationPhoto::class],
    version = 1,
    exportSchema = true,
)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        private const val NAME = "journal.db"

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
