package com.wanderwildwood.kinokocho.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * One mushroom, written down once and amended afterwards.
 *
 * A finished observation is not a goal. Three characters and a photograph is a real
 * entry; the spore print arrives the next morning and the community's name for it may
 * arrive a week later. Nothing here is required except the moment it was recorded.
 */
@Entity(tableName = "observations")
data class Observation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Generated here, in the field, and never changed.
     *
     * iNaturalist accepts a client-generated UUID on create and treats a repeat as an
     * upsert rather than a second observation. Minting it at capture time is what makes
     * a push retried days later on a bad connection safe.
     */
    @ColumnInfo(name = "uuid")
    val uuid: String,

    /** When the mushroom was found - not when the row was written. */
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "note")
    val note: String = "",

    /**
     * Where it was, in the reader's own words - "the big oak below the spring", or
     * nothing at all. Typing it is the default and needs no permission; the button that
     * fills it from the phone asks for coarse location only, and rounds what it gets.
     */
    @ColumnInfo(name = "place_note")
    val placeNote: String = "",

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    /**
     * False until the reader says to keep it.
     *
     * An observation is written to the database from the first answer, because the
     * phone is outdoors and the battery can die mid-question. That is not the same as
     * the reader having decided to keep it: most of what anyone keys out is a mushroom
     * they were curious about for a minute, and a journal holding every one of those
     * buries the finds worth going back to.
     *
     * So rows exist before they are claimed. The journal shows only claimed ones, and
     * an unclaimed row found at startup is resumed rather than listed — which is
     * exactly what should happen after a battery death: you were part-way through this
     * one.
     */
    @ColumnInfo(name = "kept", defaultValue = "0")
    val kept: Boolean = false,

    /**
     * Which vocabulary the character rows below were written against. Region packs may
     * rename or retire a character; without this, an entry from two years ago cannot be
     * read correctly by a later pack.
     */
    /**
     * What it turned out to be, once somebody said.
     *
     * The point of keeping a record is that the answer arrives later — from a forum, a
     * friend, a book at the kitchen table — and until now there was nowhere to put it.
     * An entry could hold everything about a mushroom except what it was.
     *
     * Free text and not a pack id, because the answer usually is: "a Russula, probably
     * *R. variata*", "some Cortinarius", a name that is not in this pack at all. Pinning
     * it to the pack would mean the app could only be told things it already knew.
     *
     * Deliberately not [INatLink.taxonName]. That field is what iNaturalist's community
     * said, fetched back and stamped with when it was read; this is what the reader
     * wrote down. Keeping them apart is what lets the two disagree.
     */
    @ColumnInfo(name = "identified_as")
    val identifiedAs: String = "",

    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,

    @Embedded(prefix = "inat_")
    val inat: INatLink = INatLink(),
)

/**
 * What iNaturalist knows about this observation, if it has been pushed.
 *
 * [taxonName] is the community's identification, pulled back after the fact. The app
 * itself never writes a name here - that is the whole point of the arrangement.
 */
data class INatLink(
    @ColumnInfo(name = "uuid")
    val uuid: String? = null,

    @ColumnInfo(name = "pushed_at")
    val pushedAt: Long? = null,

    @ColumnInfo(name = "taxon_id")
    val taxonId: Long? = null,

    @ColumnInfo(name = "taxon_name")
    val taxonName: String? = null,

    /**
     * When the identification above was last fetched. An identification on iNaturalist
     * can be revised, so a name without the date it was read is a name of unknown age.
     */
    @ColumnInfo(name = "identified_fetched_at")
    val identificationFetchedAt: Long? = null,
)

/**
 * One recorded character value, as a row.
 *
 * Deliberately not columns. The character schema is global but the taxon data is a
 * swappable region pack, and a pack must be able to introduce a character without a
 * database migration. [characterId] and [valueId] are stable string ids owned by the
 * schema, not enums owned by this file.
 *
 * A character may hold more than one value - a cap can be both scaly and viscid - so
 * uniqueness is on the triple, not on the character.
 *
 * Measurements ride in these rows too, folded by [MeasurementRow] into `cap_width_mm=45`,
 * for exactly the same reason: a pack must be able to add one without a migration.
 */
@Entity(
    tableName = "observation_characters",
    foreignKeys = [
        ForeignKey(
            entity = Observation::class,
            parentColumns = ["id"],
            childColumns = ["observation_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["observation_id"]),
        Index(value = ["observation_id", "character_id", "value_id"], unique = true),
    ],
)
data class ObservationCharacter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "observation_id")
    val observationId: Long,

    @ColumnInfo(name = "character_id")
    val characterId: String,

    @ColumnInfo(name = "value_id")
    val valueId: String,

    /**
     * Set when the value was added rather than when the observation was. A spore print
     * recorded the following morning is a different act from what was seen in the woods,
     * and the journal should be able to show which is which.
     */
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,
)

/**
 * A photograph attached to an observation.
 *
 * [slot] is what the picture is of. The slots exist to be nagging: an empty
 * "stipe base" is the app pointing out that the specimen was probably not dug up,
 * which is the commonest way a gilled mushroom becomes unidentifiable.
 */
@Entity(
    tableName = "observation_photos",
    foreignKeys = [
        ForeignKey(
            entity = Observation::class,
            parentColumns = ["id"],
            childColumns = ["observation_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["observation_id"])],
)
data class ObservationPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "observation_id")
    val observationId: Long,

    @ColumnInfo(name = "slot")
    val slot: String,

    /**
     * A file name, relative to the app's own private photo directory - never an
     * absolute path. The directory moves between Android versions and between a phone
     * and its restore; a stored absolute path is a broken image waiting to happen.
     */
    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,

    @ColumnInfo(name = "inat_photo_id")
    val inatPhotoId: Long? = null,
)

/** An observation with everything hanging off it, for the review screens. */
data class FullObservation(
    @Embedded val observation: Observation,

    @Relation(parentColumn = "id", entityColumn = "observation_id")
    val characters: List<ObservationCharacter> = emptyList(),

    @Relation(parentColumn = "id", entityColumn = "observation_id")
    val photos: List<ObservationPhoto> = emptyList(),
)
