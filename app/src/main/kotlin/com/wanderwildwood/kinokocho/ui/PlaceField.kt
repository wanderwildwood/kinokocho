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
    onPlaceChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(place) }
    var message by remember { mutableStateOf<String?>(null) }

    val take = {
        val where = coarsePosition(context)
        if (where == null) {
            message = "No recent position. Move outside, or type the place instead."
        } else {
            text = if (text.isBlank()) where else "$text · $where"
            onPlaceChange(text)
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
            ) { Text("Add roughly where I am") }
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
private fun coarsePosition(context: Context): String? {
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

    return "${round2(location.latitude)}, ${round2(location.longitude)}"
}

/** Two decimal places: a bit over a kilometre, which is which-hillside and no finer. */
private fun round2(value: Double): String {
    val r = (value * 100.0).roundToInt() / 100.0
    return String.format("%.2f", r)
}
