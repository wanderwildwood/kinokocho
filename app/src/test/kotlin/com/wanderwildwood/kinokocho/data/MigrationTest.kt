package com.wanderwildwood.kinokocho.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A real upgrade over a real database with somebody's notes already in it.
 *
 * JournalDatabase says there is no `fallbackToDestructiveMigration` and never will be,
 * because this holds notes that cannot be taken again — the mushroom is gone and the
 * season is over. That is a promise about a code path nothing else exercises: the
 * in-memory databases the other tests build start at the current version and never
 * migrate, so a broken migration would pass every one of them and fail on the one phone
 * that matters, holding the only copy.
 *
 * So this builds a version 3 file by hand, puts a find and a photograph in it, and opens
 * it with the app's own migration list. Room checks the resulting schema against the
 * exported 4.json itself and throws if the migration and the entity have drifted apart,
 * which means this test also asserts that the `ALTER TABLE` says exactly what
 * [ObservationPhoto.uuid] declares.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"
    private var db: JournalDatabase? = null

    @After
    fun close() {
        db?.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `a journal written at version 3 opens at version 4 with everything still in it`() = runTest {
        context.deleteDatabase(name)
        writeVersion3()

        db = Room.databaseBuilder(context, JournalDatabase::class.java, name)
            .addMigrations(
                JournalDatabase.MIGRATION_1_2,
                JournalDatabase.MIGRATION_2_3,
                JournalDatabase.MIGRATION_3_4,
            )
            .build()
        val dao = db!!.journalDao()

        val found = dao.findByUuid("kept-through-the-upgrade")
        assertNotNull("the find written at version 3 is gone", found)
        assertEquals("a note somebody wrote in a wood", found!!.observation.note)
        assertEquals(1, found.photos.size)

        /*
         * Empty rather than absent, and that is the honest value.
         *
         * This photograph was taken before anything could be published, so it has never
         * been anywhere and has no name on another server. INatPush mints one the first
         * time it sends it — before the upload, so that the picture the connection dies
         * on is matched rather than duplicated on the retry.
         */
        assertEquals("", found.photos.first().uuid)
        assertEquals("cap.jpg", found.photos.first().fileName)
    }

    /** The version 3 schema, taken verbatim from the exported 3.json. */
    private fun writeVersion3() {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        sqlite.use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `observations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`uuid` TEXT NOT NULL, `recorded_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
                    "`note` TEXT NOT NULL, `place_note` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, " +
                    "`kept` INTEGER NOT NULL DEFAULT 0, `identified_as` TEXT NOT NULL, " +
                    "`schema_version` INTEGER NOT NULL, `inat_uuid` TEXT, `inat_pushed_at` INTEGER, " +
                    "`inat_taxon_id` INTEGER, `inat_taxon_name` TEXT, `inat_identified_fetched_at` INTEGER)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `observation_characters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`observation_id` INTEGER NOT NULL, `character_id` TEXT NOT NULL, `value_id` TEXT NOT NULL, " +
                    "`recorded_at` INTEGER NOT NULL, FOREIGN KEY(`observation_id`) REFERENCES `observations`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `observation_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`observation_id` INTEGER NOT NULL, `slot` TEXT NOT NULL, `file_name` TEXT NOT NULL, " +
                    "`captured_at` INTEGER NOT NULL, `inat_photo_id` INTEGER, " +
                    "FOREIGN KEY(`observation_id`) REFERENCES `observations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_observation_characters_observation_id` " +
                    "ON `observation_characters` (`observation_id`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_observation_characters_observation_id_character_id_value_id` " +
                    "ON `observation_characters` (`observation_id`, `character_id`, `value_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_observation_photos_observation_id` " +
                    "ON `observation_photos` (`observation_id`)"
            )

            // Room refuses to open a database it did not stamp. The hash is the one in
            // 3.json; if that file ever changes, this test is meant to fail.
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf(VERSION_3_IDENTITY_HASH),
            )

            db.execSQL(
                "INSERT INTO observations (uuid, recorded_at, updated_at, note, place_note, kept, " +
                    "identified_as, schema_version) VALUES (?, 1000, 1000, ?, '', 1, '', 1)",
                arrayOf("kept-through-the-upgrade", "a note somebody wrote in a wood"),
            )
            db.execSQL(
                "INSERT INTO observation_photos (observation_id, slot, file_name, captured_at) " +
                    "VALUES (1, 'cap', 'cap.jpg', 1000)"
            )
            db.version = 3
        }
    }

    private companion object {
        /** From `app/schemas/…/3.json`. */
        const val VERSION_3_IDENTITY_HASH = "72f2fff5f307bee7d82edb1a8860d212"
    }
}
