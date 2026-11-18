package com.wanderwildwood.kinokocho.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.wanderwildwood.kinokocho.R
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Where the find was, typed or taken from the phone.
 *
 * Typing it stays the default and the app asks for nothing to do that. The button is
 * there because writing coordinates out by hand on an e-ink keyboard, in a wood, is
 * miserable — which is the same reason Birding grew one.
 *
 * **Coarse location only.** Fine location is not declared, so the app never receives a
 * precise fix rather than promising to round one it is holding; a foraging patch is
 * exactly the thing not to keep at metre precision. What is stored is rounded further,
 * to two decimal places — a bit over a kilometre — because the point is to remember
 * which hillside, not which log.
 */
@Composable
fun PlaceField(
    place: String,
    /**
     * The position already on this find, if one was ever taken. Shown under the button
     * so that a person can see what is held without it being written into their words.
     */
    latitude: Double? = null,
    longitude: Double? = null,
    onPlaceChange: (String) -> Unit,
    /**
     * The position as numbers, which for a long time went nowhere.
     *
     * The button used to format the fix into the place *text* and stop there, so
     * `Observation.latitude` and `longitude` were columns nothing ever wrote — dead
     * since the day they were added, and invisible because the coordinates were on the
     * screen the whole time, in the sentence next to them.
     */
    onPositionTaken: (Double, Double) -> Unit = { _, _ -> },
    /** On the text field itself: the entry screen keeps it on the page while it is typed in. */
    fieldModifier: Modifier = Modifier,
) {
    /*
     * The numbers are no longer written into the place note.
     *
     * They used to be appended to it — "the big oak below the spring · 35.61, -83.43" —
     * which was the only place they were kept at all, and it made a field documented as
     * "where it was, in the reader's own words" hold something that is not words. Now
     * the position is a position and the note is a sentence, and the map location is
     * enough on its own.
     *
     * Nothing migrates. Entries written before this keep their coordinates in the text,
     * because that is what somebody typed and had, and rewriting a person's own note
     * afterwards is not a thing this app does.
     */
    val context = LocalContext.current
    /*
     * Deliberately not keyed, and deliberately not keyed on [place].
     *
     * The neighbouring note and measurement fields key their state on the draft, and the
     * reflex is to do the same here. There is nothing to key on from inside this
     * function, and keying on [place] is actively wrong: the value goes back out through
     * [onPlaceChange] on every keystroke and comes back as a new [place], so the state
     * would be rebuilt on each character typed and take the cursor with it.
     *
     * What keeps it correct is that the entry screen is unmounted on the way back to the
     * journal, so this is thrown away between finds rather than following one find's
     * words onto another. That is a property of how the screens are arranged rather than
     * of this field — worth knowing before anyone gives the entry screen a way to change
     * which find it is showing without leaving it.
     */
    var text by remember { mutableStateOf(place) }
    var message by remember { mutableStateOf<String?>(null) }
    // Asking the phone for a position now, which can take a while outdoors.
    var finding by remember { mutableStateOf(false) }
    // Tied to this field: leaving the find cancels a request still in hand.
    val scope = rememberCoroutineScope()

    val noPosition = stringResource(R.string.place_no_recent_position)
    val notAllowed = stringResource(R.string.place_permission_refused)
    val findingWords = stringResource(R.string.place_finding)
    val take: () -> Unit = {
        val cached = coarsePosition(context)
        if (cached != null) {
            onPositionTaken(cached.latitude, cached.longitude)
            message = null
        } else if (!finding) {
            // Nothing recent on hand, which in a wood is the usual case: ask for one now.
            finding = true
            message = findingWords
            scope.launch {
                val where = currentCoarsePosition(context)
                finding = false
                if (where == null) {
                    message = noPosition
                } else {
                    onPositionTaken(where.latitude, where.longitude)
                    message = null
                }
            }
        }
    }

    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) take() else {
            message = notAllowed
        }
    }

    Column(Modifier.fillMaxWidth()) {
        TextFieldMMD(
            value = text,
            onValueChange = { text = it; onPlaceChange(it) },
            modifier = Modifier.fillMaxWidth().then(fieldModifier),
            placeholder = { TextMMD(stringResource(R.string.place_placeholder)) },
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButtonMMD(
                onClick = {
                    if (finding) Unit
                    else if (hasCoarse(context)) take()
                    else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(if (latitude == null) stringResource(R.string.place_take_position) else stringResource(R.string.place_take_again)) }
        }
        // What is held, said once, where the numbers used to be typed. Without this the
        // button would be the only evidence it had ever worked.
        if (latitude != null && longitude != null) {
            TextMMD(
                stringResource(R.string.place_roughly, format(latitude), format(longitude)),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        message?.let {
            TextMMD(
                it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun hasCoarse(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Rounded to two decimal places before it is ever handed on. See [round2]. */
data class Position(val latitude: Double, val longitude: Double)

/** How old a cached position may be and still be taken as where the reader is: 15 minutes. */
private const val MAX_POSITION_AGE_NANOS = 15L * 60 * 1_000_000_000

/**
 * The last position the phone already knew, rounded, when it is recent enough.
 *
 * Tried first because it costs nothing. Out on a walk nothing else has usually asked for
 * a position lately, so a null here sends [PlaceField] on to [currentCoarsePosition].
 */
private fun coarsePosition(context: Context): Position? {
    if (!hasCoarse(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val location = try {
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .asSequence()
            .mapNotNull { provider ->
                @Suppress("MissingPermission")
                if (manager.allProviders.contains(provider)) {
                    manager.getLastKnownLocation(provider)
                } else {
                    null
                }
            }
            .maxByOrNull { it.elapsedRealtimeNanos }
    } catch (_: SecurityException) {
        null
    } ?: return null
    // A cached position from hours ago is likely somewhere else, home most of all: only a
    // recent one is "roughly where I am". Otherwise a position is asked for now.
    val age = android.os.SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
    if (age > MAX_POSITION_AGE_NANOS) return null

    // Rounded here, once, before anything else in the app can see the finer number.
    // Rounding at the point of display would leave the precise value sitting in a field
    // somewhere, which is the arrangement this app has said it does not have.
    return Position(round2(location.latitude), round2(location.longitude))
}

/** How long to wait for a position asked for now before saying there is none: 20 seconds. */
private const val FRESH_POSITION_TIMEOUT_MS = 20_000L

/**
 * A position asked for now, rounded, or null when none came within the timeout.
 *
 * Every provider that is on is asked at once, the network provider and GPS, and the first
 * one with an answer wins; the other request is cancelled. In a wood the network provider
 * can be on and still find nothing, with no cell or Wi-Fi in reach, while GPS can. With
 * coarse permission alone the system blurs whatever GPS finds before the app sees it, so
 * this asks for nothing more precise than the one permission allows.
 */
private suspend fun currentCoarsePosition(context: Context): Position? {
    if (!hasCoarse(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (providers.isEmpty()) return null
    val location = withTimeoutOrNull(FRESH_POSITION_TIMEOUT_MS) {
        firstAnswer<Location>(providers) { provider, answer ->
            val signal = CancellationSignal()
            try {
                @Suppress("MissingPermission")
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { found ->
                    answer(found)
                }
            } catch (_: SecurityException) {
                answer(null)
            } catch (_: IllegalArgumentException) {
                answer(null)
            }
            return@firstAnswer { signal.cancel() }
        }
    } ?: return null
    return Position(round2(location.latitude), round2(location.longitude))
}

/**
 * Asks every one of [sources] at once and gives back the first answer that is not null,
 * or null once all of them have answered null. [ask] starts one request, hands its answer
 * to the callback it is given, and returns a way to cancel that request. When one source
 * answers, or when the caller is cancelled (the timeout, or the field being left), every
 * request still out is cancelled. A source asked after the race is already over is not
 * asked at all.
 */
internal suspend fun <T : Any> firstAnswer(
    sources: List<String>,
    ask: (source: String, answer: (T?) -> Unit) -> () -> Unit,
): T? {
    if (sources.isEmpty()) return null
    return suspendCancellableCoroutine { cont ->
        val over = AtomicBoolean(false)
        val waiting = AtomicInteger(sources.size)
        val cancels = CopyOnWriteArrayList<() -> Unit>()
        fun cancelAll() = cancels.forEach { cancel -> runCatching { cancel() } }
        cont.invokeOnCancellation {
            over.set(true)
            cancelAll()
        }
        for (source in sources) {
            if (over.get()) break
            val cancel = ask(source) { found ->
                val last = waiting.decrementAndGet() == 0
                if ((found != null || last) && over.compareAndSet(false, true)) {
                    cancelAll()
                    if (cont.isActive) cont.resume(found)
                }
            }
            cancels.add(cancel)
            // An answer that came back before its cancel was known: cancel this one too.
            if (over.get()) runCatching { cancel() }
        }
    }
}

/** Two decimal places: a bit over a kilometre, which is which-hillside and no finer. */
private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0

private fun format(value: Double): String = String.format("%.2f", value)
