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
import com.wanderwildwood.kinokocho.key.KeyData
import com.wanderwildwood.kinokocho.key.TaxonPack
import com.wanderwildwood.kinokocho.schema.Character
import com.wanderwildwood.kinokocho.schema.CharacterSchema
import com.wanderwildwood.kinokocho.ui.JournalExport
import com.wanderwildwood.kinokocho.ui.JournalImport
import com.wanderwildwood.kinokocho.ui.PhotoExif
import com.wanderwildwood.kinokocho.ui.photoDir
import java.io.File
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class JournalViewModel(app: Application) : AndroidViewModel(app) {

    // Read off the main thread by the activity before it builds this; see KeyData.
    private val key = KeyData.get(app.assets)
    val schema: CharacterSchema = key.schema
    val pack: TaxonPack = key.pack
    val engine: KeyEngine = key.engine

    private val dao = JournalDatabase.get(app).journalDao()

    init {
        /*
         * Versions up to 0.3.40 could sign in to iNaturalist and kept the sign-in (an access
         * token, a day-long API token, the user name and the location setting) in the
         * "inaturalist" preferences. Publishing is gone and nothing reads them, so they are
         * deleted rather than left on the phone. A no-op on every later start.
         */
        app.deleteSharedPreferences("inaturalist")
    }

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
        /**
         * Where it was as numbers, when the reader pressed the button for it.
         *
         * Null is the ordinary case and not a gap: typing the place is the default and
         * asks for no permission.
         */
        val latitude: Double? = null,
        val longitude: Double? = null,
        val photos: List<ObservationPhoto> = emptyList(),
        /** Set when the reader has stepped back to revisit a question already answered. */
        val revisiting: String? = null,
        /** Already in the journal: changes to it are saved as they are made, nothing to ask. */
        val kept: Boolean = false,
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
        commitTrash()
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
                latitude = full.observation.latitude,
                longitude = full.observation.longitude,
                photos = full.photos,
                kept = full.observation.kept,
            )
        }
    }

    fun close() {
        _draft.value = null
    }

    /** The reader has said to keep this one. Until now it was only written down. */
    fun keep() {
        val d = _draft.value?.copy(kept = true) ?: return
        _draft.value = d
        viewModelScope.launch {
            // A find with nothing answered yet has no row: it is written down first, so what
            // is added after "Save it" goes into the journal with it. From the find as it was
            // when Save it was pressed, so closing it straight after still keeps it.
            val id = persistNow(d) ?: return@launch
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
        commitTrash()
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

    /**
     * Writes down where the phone says it is, already rounded by [ui.PlaceField].
     *
     * Rounded before it arrives rather than here, so no part of this app ever holds a
     * position finer than the one it promises to keep.
     */
    fun setPosition(latitude: Double, longitude: Double) {
        _draft.value = _draft.value?.copy(latitude = latitude, longitude = longitude)
        persist()
    }

    /**
     * A photograph the camera has just written: its place taken out, off the main thread,
     * then added to the find it was taken for. One that cannot be made clean is kept all
     * the same, since it cannot be taken again; a share leaves it out.
     */
    fun takeInPhoto(slot: String, fileName: String) {
        val uuid = _draft.value?.uuid ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { PhotoExif.strip(File(photoDir(getApplication()), fileName)) }
            addPhoto(slot, fileName, uuid)
        }
    }

    /**
     * A picture picked from the gallery: copied into the app's own folder and its place
     * taken out, off the main thread. One that cannot be read, or cannot be made clean, is
     * not taken: the person still has it where it came from, and [onRefused] says so.
     */
    fun takeInPicked(slot: String, uri: android.net.Uri, onRefused: () -> Unit) {
        val uuid = _draft.value?.uuid ?: return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val fileName = "${UUID.randomUUID()}.jpg"
            val taken = withContext(Dispatchers.IO) {
                val file = File(photoDir(app), fileName)
                val copied = runCatching {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use(input::copyTo)
                    } != null
                }.getOrDefault(false)
                val ok = copied && PhotoExif.strip(file) != PhotoExif.Outcome.FAILED
                if (!ok) file.delete()
                ok
            }
            if (taken) addPhoto(slot, fileName, uuid) else onRefused()
        }
    }

    /** Only onto the find it was taken for, if that is still the one open. */
    private fun addPhoto(slot: String, fileName: String, uuid: String) {
        val d = _draft.value?.takeIf { it.uuid == uuid } ?: return
        val photo = ObservationPhoto(
            observationId = d.observationId ?: 0,
            slot = slot,
            fileName = fileName,
            capturedAt = System.currentTimeMillis(),
            // Minted at capture, like the observation's own, so that a push interrupted
            // by a lost connection can be run again without posting the picture twice.
            uuid = UUID.randomUUID().toString(),
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
        // The find as it is now, not as it is when this save's turn comes: a find closed
        // while earlier saves are still being written keeps its last edit.
        val d = _draft.value ?: return
        viewModelScope.launch { persistNow(d) }
    }

    /**
     * One write to the journal at a time, in the order they were asked for: saves, and the
     * deletes that could otherwise land in the middle of one.
     */
    private val saving = Mutex()

    /**
     * The row each find got when it was first written, by uuid. A save queued before the
     * first insert came back has no id in its copy of the find, and must not make a second
     * row. Only touched under [saving].
     */
    private val rowIds = mutableMapOf<String, Long>()

    /** Finds thrown away. A save still queued for one writes nothing. Under [saving]. */
    private val discarded = mutableSetOf<String>()

    /**
     * Writes [d] down and returns its row's id, or null when there is nothing to write to:
     * the find was thrown away or deleted while this save waited.
     */
    private suspend fun persistNow(d: Draft): Long? = saving.withLock {
        if (d.uuid in discarded) return@withLock null
        val now = System.currentTimeMillis()
        val id = d.observationId ?: rowIds[d.uuid] ?: dao.insert(
            Observation(
                uuid = d.uuid,
                recordedAt = d.recordedAt,
                updatedAt = now,
                note = d.note,
                placeNote = d.placeNote,
                identifiedAs = d.identifiedAs,
                latitude = d.latitude,
                longitude = d.longitude,
                kept = d.kept,
                schemaVersion = schema.version,
            )
        ).also { newId ->
            rowIds[d.uuid] = newId
            // Only onto the same find: another may have been opened while this was written.
            _draft.update {
                if (it != null && it.uuid == d.uuid && it.observationId == null) it.copy(observationId = newId) else it
            }
        }

        val rows = mutableListOf<ObservationCharacter>()
        // Replace the character rows wholesale. There are a few dozen at most, and
        // reconciling them individually would be more code for no gain.
        d.answers.values.forEach { (characterId, values) ->
            values.forEach { value -> rows += ObservationCharacter(0, id, characterId, value, now) }
        }
        // Written down, not just remembered. See [MeasurementRow.NOT_TESTED].
        d.answers.notTested.forEach { characterId ->
            rows += ObservationCharacter(0, id, characterId, MeasurementRow.NOT_TESTED, now)
        }
        // Measurements go down the same rows, as `cap_width_mm=45`. Not a column,
        // for the reason the character rows are not columns either: a region pack
        // has to be able to introduce one without a database migration, and a
        // number is no more special than a state in that respect.
        measurementCharacters.forEach { character ->
            val ids = schema.valuesOf(character).map { it.id }
            d.answers.measurements.filterKeys { it in ids }.forEach { (key, mm) ->
                rows += ObservationCharacter(0, id, character.id, MeasurementRow.encode(key, mm), now)
            }
        }
        val saved = dao.saveFind(
            observationId = id,
            note = d.note,
            placeNote = d.placeNote,
            identifiedAs = d.identifiedAs,
            latitude = d.latitude,
            longitude = d.longitude,
            updatedAt = now,
            characters = rows,
            photos = d.photos,
        )
        if (saved) id else null
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
        viewModelScope.launch {
            // Behind any save already queued, which may still carry this photograph.
            saving.withLock {
                val observationId = d.observationId ?: rowIds[d.uuid]
                if (observationId != null) dao.deletePhotoNamed(observationId, photo.fileName)
                runCatching { File(photoDir(getApplication()), photo.fileName).delete() }
            }
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
        // Not in viewModelScope. Back from the journal stops the activity, which lets a
        // pending delete go, and clears this view model a moment later, which cancels its
        // scope: a delete still waiting for [saving] or for Room then would be dropped,
        // perhaps after some of the photographs were already gone. Undispatched, so it
        // takes its place in the queue now, in the order it was asked for.
        lasting.launch(start = CoroutineStart.UNDISPATCHED) {
            saving.withLock { deleteRow(observationId) }
        }
    }

    /**
     * Throws away the find being keyed, one that was never kept, and closes it.
     *
     * Behind any save already queued for it, and marked so that none queued later writes
     * it back: a find answered and then thrown away at once must not come back as the
     * find being keyed on the next start.
     */
    fun discard() {
        val d = _draft.value ?: return
        _draft.value = null
        if (d.kept) return
        viewModelScope.launch {
            saving.withLock {
                discarded += d.uuid
                val id = d.observationId ?: rowIds[d.uuid] ?: return@withLock
                deleteRow(id)
            }
        }
    }

    /** The row and its photographs. Only under [saving]. */
    private suspend fun deleteRow(observationId: Long) {
        val dir = photoDir(getApplication())
        dao.photosOf(observationId).forEach {
            runCatching { File(dir, it.fileName).delete() }
        }
        dao.deleteObservation(observationId)
    }

    /**
     * The find just deleted from the journal, hidden but still whole until it is let go.
     *
     * Deleting from the journal hides the row and offers Undo; nothing is removed from disk
     * until the person moves on (opens or starts a find, deletes another, or leaves the
     * app). Only if the process dies before any of those does the find come back. A backup
     * made while Undo is showing already leaves it out.
     */
    private val _trashed = MutableStateFlow<Long?>(null)
    val trashed: StateFlow<Long?> = _trashed.asStateFlow()

    fun trash(observationId: Long) {
        commitTrash()
        _trashed.value = observationId
    }

    fun undoTrash() {
        _trashed.value = null
    }

    /** Makes a pending delete final: the row and its photographs go. */
    fun commitTrash() {
        val id = _trashed.value ?: return
        _trashed.value = null
        delete(id)
    }

    /**
     * A view model cleared with a delete still pending, which leaving the app normally
     * commits first (see [commitTrash]). [delete] runs in [lasting], not in the scope
     * cancelled here, still behind any save in hand.
     */
    override fun onCleared() {
        commitTrash()
    }

    private companion object {
        /** Outlives the view model, for the deletes that must not be cut off halfway. */
        val lasting = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private val measurementCharacters
        get() = schema.characters.filter { it.kind == Character.Kind.MEASUREMENT }

    private fun isMeasurement(characterId: String): Boolean =
        schema.character(characterId)?.kind == Character.Kind.MEASUREMENT

    private val backingUp = Mutex()

    /**
     * Writes the whole journal out, and hands back the file to be shared.
     *
     * On a background thread because it copies every photograph, and reported through a
     * callback rather than a state flag because the only thing waiting on it is a share
     * sheet. Nothing here can alter the journal — the property that matters most in a
     * thing somebody reaches for when something has already gone wrong.
     */
    /** Writes the whole journal as a zip into the place the person picked. */
    fun exportJournalTo(uri: android.net.Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            // One backup at a time: a second, to the same place or another, waits for the
            // first rather than running beside it.
            val ok = backingUp.withLock {
                runCatching {
                    val app = getApplication<Application>()
                    // Not the find deleted a moment ago with its Undo still showing.
                    val trashed = _trashed.value
                    val kept = dao.allKept().filter { it.observation.id != trashed }
                    // Zipping every photograph takes seconds: off the main thread, which it was not.
                    withContext(Dispatchers.IO) {
                        val file = JournalExport.write(app, kept)
                        try {
                            // "wt": over an older backup, "w" alone can leave that file's tail behind.
                            val out = runCatching { app.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
                                ?: app.contentResolver.openOutputStream(uri)
                                ?: error("no stream")
                            out.use { o -> file.inputStream().use { it.copyTo(o) } }
                        } finally {
                            file.delete()
                        }
                    }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    /**
     * Reads a backup back in, adding only what is not already here.
     *
     * See [JournalImport]: nothing is deleted and nothing is overwritten, so restoring an
     * old backup onto a phone that has newer finds on it keeps both.
     */
    fun importJournal(uri: android.net.Uri, onDone: (JournalImport.Result) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { JournalImport.read(getApplication(), uri, dao) }
                .getOrElse { JournalImport.Result(failed = getApplication<Application>().getString(R.string.import_could_not_read)) }
            onDone(result)
        }
    }

    private fun monthOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MONTH) + 1

}
