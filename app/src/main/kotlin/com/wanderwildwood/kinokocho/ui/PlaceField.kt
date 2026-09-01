package com.wanderwildwood.kinokocho.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import kotlin.math.roundToInt

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
     * screen the whole time, in the sentence next to them. It stopped being invisible
     * when publishing was built: iNaturalist cannot make an observation research grade
     * without a location, so every find would have gone up destined to be ignored, and
     * the app would have looked like it was working.
     */
    onPositionTaken: (Double, Double) -> Unit = { _, _ -> },
) {
    /*
     * The numbers are no longer written into the place note.
     *
     * They used to be appended to it — "the big oak below the spring · 35.89, -82.83" —
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

    val take = {
        val where = coarsePosition(context)
        if (where == null) {
            message = "No recent position. Move outside, or type the place instead."
        } else {
            onPositionTaken(where.latitude, where.longitude)
            message = null
        }
    }

    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) take() else {
            message = "Not allowed. Type the place instead — it works just as well."
        }
    }

    Column(Modifier.fillMaxWidth()) {
        TextFieldMMD(
            value = text,
            onValueChange = { text = it; onPlaceChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("The place, in your own words") },
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButtonMMD(
                onClick = {
                    if (hasCoarse(context)) take()
                    else ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (latitude == null) "Add roughly where I am" else "Take it again") }
        }
        // What is held, said once, where the numbers used to be typed. Without this the
        // button would be the only evidence it had ever worked.
        if (latitude != null && longitude != null) {
            Text(
                "Roughly ${format(latitude)}, ${format(longitude)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        message?.let {
            Text(
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

/**
 * The last position the phone already knew, rounded.
 *
 * Deliberately does not ask for a fresh fix: that spins the radio, takes time a person
 * standing in the rain does not want to give, and buys precision this app has decided
 * not to keep. If nothing is cached the honest answer is that there is no position.
 */
/** Rounded to two decimal places before it is ever handed on. See [round2]. */
data class Position(val latitude: Double, val longitude: Double)

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
            .maxByOrNull { it.time }
    } catch (_: SecurityException) {
        null
    } ?: return null

    // Rounded here, once, before anything else in the app can see the finer number.
    // Rounding at the point of display would leave the precise value sitting in a field
    // somewhere, which is the arrangement this app has said it does not have.
    return Position(round2(location.latitude), round2(location.longitude))
}

/** Two decimal places: a bit over a kilometre, which is which-hillside and no finer. */
private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0

private fun format(value: Double): String = String.format("%.2f", value)
