package com.wanderwildwood.kinokocho.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert
    suspend fun insert(observation: Observation): Long

    @Update
    suspend fun update(observation: Observation)

    @Query("DELETE FROM observations WHERE id = :observationId")
    suspend fun deleteObservation(observationId: Long)

    /** The journal: what the reader chose to keep, newest first. */
    @Transaction
    @Query("SELECT * FROM observations WHERE kept = 1 ORDER BY recorded_at DESC")
    fun observeAll(): Flow<List<FullObservation>>

    /**
     * Everything kept, once, for writing out.
     *
     * The flow above is what the journal watches; this is what a backup reads. A backup
     * that subscribed to a flow would be a backup that could change under itself
     * halfway through writing.
     */
    @Transaction
    @Query("SELECT * FROM observations WHERE kept = 1 ORDER BY recorded_at ASC")
    suspend fun allKept(): List<FullObservation>

    /**
     * A find that was being keyed out when the app stopped.
     *
     * There is at most one worth caring about — the most recent — and finding it at
     * startup means resuming it rather than starting again. This is what a dead battery
     * mid-question should cost: nothing.
     */
    @Transaction
    @Query("SELECT * FROM observations WHERE kept = 0 ORDER BY updated_at DESC LIMIT 1")
    suspend fun findUnclaimed(): FullObservation?

    @Query("UPDATE observations SET kept = 1 WHERE id = :observationId")
    suspend fun keep(observationId: Long)

    /** Unclaimed rows other than the one in hand: leftovers from older crashes. */
    @Query("DELETE FROM observations WHERE kept = 0 AND id != :except")
    suspend fun clearOtherUnclaimed(except: Long)

    @Transaction
    @Query("SELECT * FROM observations WHERE id = :observationId")
    fun observeOne(observationId: Long): Flow<FullObservation?>

    @Transaction
    @Query("SELECT * FROM observations WHERE id = :observationId")
    suspend fun findOne(observationId: Long): FullObservation?

    @Transaction
    @Query("SELECT * FROM observations WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): FullObservation?

    /**
     * Recording the same character value twice is not an error, it is a person tapping
     * a row again. IGNORE makes that idempotent against the unique index rather than
     * throwing, and leaves the original recorded_at alone - which is correct, because
     * the first time it was noticed is the true answer.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCharacter(character: ObservationCharacter): Long

    @Query(
        "DELETE FROM observation_characters " +
            "WHERE observation_id = :observationId AND character_id = :characterId " +
            "AND value_id = :valueId"
    )
    suspend fun removeCharacterValue(observationId: Long, characterId: String, valueId: String)

    /** For a single-valued character, replacing the answer rather than adding to it. */
    @Query(
        "DELETE FROM observation_characters " +
            "WHERE observation_id = :observationId AND character_id = :characterId"
    )
    suspend fun clearCharacter(observationId: Long, characterId: String)

    @Transaction
    suspend fun setSingleCharacter(
        observationId: Long,
        characterId: String,
        valueId: String,
        recordedAt: Long,
    ) {
        clearCharacter(observationId, characterId)
        addCharacter(ObservationCharacter(0, observationId, characterId, valueId, recordedAt))
    }

    @Query("SELECT * FROM observation_characters WHERE observation_id = :observationId")
    suspend fun charactersOf(observationId: Long): List<ObservationCharacter>

    @Insert
    suspend fun addPhoto(photo: ObservationPhoto): Long

    @Query("DELETE FROM observation_photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: Long)

    /**
     * By file name, because a photograph taken in this session has no id yet.
     *
     * The draft holds rows built in memory and [addPhoto] returns the new id without
     * anyone writing it back, so a picture taken a moment ago still carries id 0.
     * Deleting by id would silently leave that row behind, pointing at a file that had
     * just been removed — the photograph would come back as "(missing)" the next time
     * the entry was opened. The file name is minted per photograph and never reused.
     */
    @Query("DELETE FROM observation_photos WHERE observation_id = :observationId AND file_name = :fileName")
    suspend fun deletePhotoNamed(observationId: Long, fileName: String)

    @Query("SELECT * FROM observation_photos WHERE observation_id = :observationId")
    suspend fun photosOf(observationId: Long): List<ObservationPhoto>

    /** Everything not yet pushed, oldest first, for the review-and-push screen at home. */
    @Transaction
    @Query("SELECT * FROM observations WHERE inat_uuid IS NULL ORDER BY recorded_at ASC")
    suspend fun notYetPushed(): List<FullObservation>

    /**
     * Pushed but with no identification read back yet. This is the list the app asks
     * iNaturalist about; it never asks about anything it has not published.
     */
    @Query("SELECT * FROM observations WHERE inat_uuid IS NOT NULL AND inat_taxon_id IS NULL")
    suspend fun awaitingIdentification(): List<Observation>

    @Query("SELECT COUNT(*) FROM observations")
    suspend fun count(): Int
}
