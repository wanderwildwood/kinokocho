package com.wanderwildwood.kinokocho

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanderwildwood.kinokocho.data.FullObservation
import com.wanderwildwood.kinokocho.data.JournalDatabase
import com.wanderwildwood.kinokocho.data.MeasurementRow
import com.wanderwildwood.kinokocho.data.Observation
import com.wanderwildwood.kinokocho.data.ObservationCharacter
import com.wanderwildwood.kinokocho.data.ObservationPhoto
import com.wanderwildwood.kinokocho.key.KeyEngine
import com.wanderwildwood.kinokocho.key.PackLoader
import com.wanderwildwood.kinokocho.key.TaxonPack
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.CharacterSchema
import com.wanderwildwood.kinokocho.schema.SchemaLoader
import com.wanderwildwood.kinokocho.ui.JournalExport
import com.wanderwildwood.kinokocho.ui.photoDir
import java.io.File
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(app: Application) : AndroidViewModel(app) {

    val schema: CharacterSchema = SchemaLoader.load(app.assets)
    val pack: TaxonPack = PackLoader.load(app.assets, PACK)
    val engine = KeyEngine(schema, pack)

    private val dao = JournalDatabase.get(app).journalDao()

    val entries: StateFlow<List<FullObservation>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The entry being written or looked at, held in memory until it is saved. */
    private val _draft = MutableStateFlow<Draft?>(null)
    val draft: StateFlow<Draft?> = _draft.asStateFlow()

    data class Draft(
        val observationId: Long?,
        val uuid: String,
        val recordedAt: Long,
        val answers: KeyEngine.Answers,
        val note: String = "",
        val placeNote: String = "",
        /** What it turned out to be, once somebody said. Free text. */
        val identifiedAs: String = "",
        val photos: List<ObservationPhoto> = emptyList(),
        /** Set when the reader has stepped back to revisit a question already answered. */
        val revisiting: String? = null,
    )

    fun startNewEntry() {
        val now = System.currentTimeMillis()
        _draft.value = Draft(
            observationId = null,
            uuid = UUID.randomUUID().toString(),
            recordedAt = now,
            answers = KeyEngine.Answers(month = monthOf(now)),
        )
    }

    fun open(observationId: Long) {
        viewModelScope.launch {
            val full = dao.findOne(observationId) ?: return@launch
            _draft.value = Draft(
                observationId = full.observation.id,
                uuid = full.observation.uuid,
                recordedAt = full.observation.recordedAt,
                answers = KeyEngine.Answers(
                    values = full.characters.filterNot { isMeasurement(it.characterId) }
                        .filterNot { it.valueId == MeasurementRow.NOT_TESTED }
                        .groupBy { it.characterId }
                        .mapValues { (_, rows) -> rows.map { it.valueId }.toSet() },
                    notTested = full.characters
                        .filter { it.valueId == MeasurementRow.NOT_TESTED }
                        .map { it.characterId }.toSet(),
                    measurements = full.characters.filter { isMeasurement(it.characterId) }
                        .mapNotNull { MeasurementRow.decode(it.valueId) }
                        .toMap(),
                    month = monthOf(full.observation.recordedAt),
                ),
                note = full.observation.note,
                placeNote = full.observation.placeNote,
                identifiedAs = full.observation.identifiedAs,
                photos = full.photos,
            )
        }
    }

    fun close() {
        _draft.value = null
    }

    /** The reader has said to keep this one. Until now it was only written down. */
    fun keep() {
        val id = _draft.value?.observationId ?: return
        viewModelScope.launch {
            dao.keep(id)
            // Anything else still unclaimed is a leftover from a crash older than this
            // one, and resuming two finds at once is not a thing a person can do.
            dao.clearOtherUnclaimed(id)
        }
    }

    /**
     * Picks up a find that was being keyed out when the app stopped, or starts a new one.
     *
     * A battery dying mid-question should cost nothing, so the answers already given are
     * waiting where they were left.
     */
    fun resumeOrStart() {
        viewModelScope.launch {
            val unclaimed = dao.findUnclaimed()
            if (unclaimed == null) {
                startNewEntry()
            } else {
                open(unclaimed.observation.id)
            }
        }
    }

    /** The question to put in front of the reader, or null when there is nothing left to ask. */
    fun currentQuestion(draft: Draft): String? =
        draft.revisiting ?: engine.nextQuestion(draft.answers)

    fun answer(characterId: String, chosen: Set<String>) {
        val d = _draft.value ?: return
        val answers = if (chosen.isEmpty()) {
            d.answers.copy(values = d.answers.values - characterId)
        } else {
            d.answers.with(characterId, chosen)
        }
        _draft.value = d.copy(answers = answers, revisiting = null)
        persist()
    }

    /**
     * Records a set of measurements, in millimetres, and moves the key on.
     *
     * Taken together rather than one at a time because they are read off one mushroom
     * in one go, and a key that asked for the cap, then something else, then the stem
     * would have the reader put it down and pick it up twice.
     */
    fun measure(millimetres: Map<String, Int?>) {
        val d = _draft.value ?: return
        var answers = d.answers
        millimetres.forEach { (valueId, mm) -> answers = answers.withMeasurement(valueId, mm) }
        _draft.value = d.copy(answers = answers, revisiting = null)
        persist()
    }

    fun skip(characterId: String) {
        val d = _draft.value ?: return
        _draft.value = d.copy(answers = d.answers.markNotTested(characterId), revisiting = null)
        persist()
    }

    fun revisit(characterId: String) {
        _draft.value = _draft.value?.copy(revisiting = characterId)
    }

    fun setNote(text: String) {
        _draft.value = _draft.value?.copy(note = text)
        persist()
    }

    fun setPlaceNote(text: String) {
        _draft.value = _draft.value?.copy(placeNote = text)
        persist()
    }

    /**
     * Writes down what it turned out to be.
     *
     * The answer arrives after the walk — from a forum, a friend, a book at the kitchen
     * table — and there was nowhere to put it. An entry could hold everything about a
     * mushroom except what it was.
     */
    fun setIdentifiedAs(text: String) {
        _draft.value = _draft.value?.copy(identifiedAs = text)
        persist()
    }

    fun addPhoto(slot: String, fileName: String) {
        val d = _draft.value ?: return
        val photo = ObservationPhoto(
            observationId = d.observationId ?: 0,
            slot = slot,
            fileName = fileName,
            capturedAt = System.currentTimeMillis(),
        )
        _draft.value = d.copy(photos = d.photos + photo)
        persist()
    }

    /**
     * Writes the draft down.
     *
     * Called after every tap rather than behind a Save button, because the phone is
     * outdoors and the entry has to survive the battery dying mid-question. The
     * observation's uuid is minted once and never changes, so re-saving updates the
     * same row rather than making a second mushroom.
     */
    private fun persist() {
        val d = _draft.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = d.observationId ?: dao.insert(
                Observation(
                    uuid = d.uuid,
                    recordedAt = d.recordedAt,
                    updatedAt = now,
                    note = d.note,
                    placeNote = d.placeNote,
                    identifiedAs = d.identifiedAs,
                    schemaVersion = schema.version,
                )
            ).also { newId -> _draft.value = _draft.value?.copy(observationId = newId) }

            dao.findOne(id)?.let { existing ->
                dao.update(
                    existing.observation.copy(
                        note = d.note,
                        placeNote = d.placeNote,
                        identifiedAs = d.identifiedAs,
                        updatedAt = now,
                    )
                )
            }

            // Replace the character rows wholesale. There are a few dozen at most, and
            // reconciling them individually would be more code for no gain.
            dao.charactersOf(id).map { it.characterId }.distinct().forEach {
                dao.clearCharacter(id, it)
            }
            d.answers.values.forEach { (characterId, values) ->
                values.forEach { value ->
                    dao.addCharacter(ObservationCharacter(0, id, characterId, value, now))
                }
            }

            // Written down, not just remembered. See [MeasurementRow.NOT_TESTED].
            d.answers.notTested.forEach { characterId ->
                dao.addCharacter(
                    ObservationCharacter(
                        0, id, characterId, MeasurementRow.NOT_TESTED, now,
                    )
                )
            }

            // Measurements go down the same rows, as `cap_width_mm=45`. Not a column,
            // for the reason the character rows are not columns either: a region pack
            // has to be able to introduce one without a database migration, and a
            // number is no more special than a state in that respect.
            measurementCharacters.forEach { character ->
                val ids = schema.valuesOf(character).map { it.id }
                d.answers.measurements.filterKeys { it in ids }.forEach { (key, mm) ->
                    dao.addCharacter(
                        ObservationCharacter(
                            0, id, character.id, MeasurementRow.encode(key, mm), now,
                        )
                    )
                }
            }

            val known = dao.photosOf(id).map { it.fileName }.toSet()
            d.photos.filter { it.fileName !in known }.forEach {
                dao.addPhoto(it.copy(observationId = id))
            }
        }
    }

    /**
     * Takes a photograph back out, file and all.
     *
     * The camera goes out to whatever app the phone has, so a picture arrives however it
     * came out — a thumb over the lens, the base still in the ground, the wrong mushroom
     * entirely. There was no way to remove one: [JournalDao.deletePhoto] existed and
     * nothing ever called it, so the first bad photograph in a slot stayed there.
     *
     * The file goes too. A journal entry is a small thing and a photograph is not, and
     * leaving the picture on disk after the reader has said to remove it is keeping
     * something they asked to be rid of.
     */
    fun deletePhoto(photo: ObservationPhoto) {
        val d = _draft.value ?: return
        _draft.value = d.copy(photos = d.photos.filterNot { it.fileName == photo.fileName })
        val observationId = d.observationId
        viewModelScope.launch {
            if (observationId != null) dao.deletePhotoNamed(observationId, photo.fileName)
            runCatching { File(photoDir(getApplication()), photo.fileName).delete() }
        }
    }

    /**
     * Removes a find from the journal, photographs and all.
     *
     * The files used to stay on disk for ever: the database rows cascade and nothing
     * touched the pictures, so a deleted entry left its photographs behind — while
     * PRIVACY.md said they went with it. A promise about where somebody forages is not a
     * promise to be approximately true.
     *
     * This is deletion, not discard. Throwing away a find that was never kept leaves its
     * photographs alone on purpose, so that a mis-tap at the end of a walk costs the
     * record and not the pictures; deleting one from the journal is a thing a person did
     * on purpose, twice.
     */
    fun delete(observationId: Long) {
        viewModelScope.launch {
            val dir = photoDir(getApplication())
            dao.photosOf(observationId).forEach {
                runCatching { File(dir, it.fileName).delete() }
            }
            dao.deleteObservation(observationId)
        }
    }

    private val measurementCharacters
        get() = schema.characters.filter { it.kind == Character.Kind.MEASUREMENT }

    private fun isMeasurement(characterId: String): Boolean =
        schema.character(characterId)?.kind == Character.Kind.MEASUREMENT

    /**
     * Writes the whole journal out, and hands back the file to be shared.
     *
     * On a background thread because it copies every photograph, and reported through a
     * callback rather than a state flag because the only thing waiting on it is a share
     * sheet. Nothing here can alter the journal — the property that matters most in a
     * thing somebody reaches for when something has already gone wrong.
     */
    fun exportJournal(onReady: (java.io.File?) -> Unit) {
        viewModelScope.launch {
            val file = runCatching {
                JournalExport.write(getApplication(), dao.allKept())
            }.getOrNull()
            onReady(file)
        }
    }

    private fun monthOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MONTH) + 1

    private companion object {
        const val PACK = "packs/southern-appalachia-v1.json"
    }
}
