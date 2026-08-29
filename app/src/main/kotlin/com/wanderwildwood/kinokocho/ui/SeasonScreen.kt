package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.wanderwildwood.kinokocho.R
import com.wanderwildwood.kinokocho.key.Hazard
import com.wanderwildwood.kinokocho.key.Taxon
import java.text.DateFormatSymbols
import java.util.Locale

/**
 * What the pack expects to be about this month.
 *
 * A calendar, not an identification. It says nothing about the mushroom in anyone's
 * hand — it says what season it is, which is the one thing a journal can tell you
 * before you have found anything at all, and the thing that decides where to walk.
 *
 * Deliberately offline and location-free. The apps that do this properly read live
 * weather for a place, which would cost this app both the INTERNET permission and a
 * GPS fix, and a foraging spot is exactly the thing not to hand to a weather service.
 * The month and the region pack are already here and cost nothing.
 *
 * **The dangerous ones are listed, and listed first.** A month view that quietly left
 * out the Amanitas would read as a list of things to pick, which is the one thing this
 * app must never produce.
 */
@Composable
fun SeasonScreen(taxa: List<Taxon>, month: Int, onClose: () -> Unit) {
    val inSeason = taxa.filter { it.seasonMonths.isEmpty() || month in it.seasonMonths }
    val dangerous = inSeason.filter { it.hazard.severity.alwaysShow }
        .sortedBy { it.scientificName }
    val rest = (inSeason - dangerous.toSet()).sortedBy { it.scientificName }
    val monthName = DateFormatSymbols(Locale.getDefault()).months[month - 1]

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
    ) {
        item {
            Text(
                "About in $monthName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "${inSeason.size} of ${taxa.size} in this region pack have been recorded " +
                    "in $monthName. Seasons are a guide and nothing more — fungi do not " +
                    "read calendars, and an out-of-season find is worth writing down " +
                    "precisely because it is one.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }

        if (dangerous.isNotEmpty()) {
            item {
                HorizontalDividerMMD(Modifier.padding(top = 10.dp))
                Text(
                    "Worth knowing this month",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(dangerous.size) { i -> TaxonLine(dangerous[i], danger = true) }
        }

        item {
            HorizontalDividerMMD(Modifier.padding(top = 10.dp))
            Text(
                "Also about",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }
        items(rest.size) { i -> TaxonLine(rest[i], danger = false) }

        item {
            OutlinedButtonMMD(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) { Text("Close") }
        }
    }
}

/**
 * One taxon, named and nothing more.
 *
 * No edibility, no "good find", no ranking. For the dangerous ones the severity and
 * the one-line note are shown, because that is information a person needs before they
 * are holding it rather than after.
 */
@Composable
private fun TaxonLine(taxon: Taxon, danger: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            taxon.commonName?.let { "${taxon.scientificName} — $it" }
                ?: taxon.scientificName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (danger) FontWeight.Bold else FontWeight.Normal,
        )
        if (danger) {
            Text(
                when (taxon.hazard.severity) {
                    Hazard.Severity.LETHAL -> "Can kill."
                    Hazard.Severity.SEVERE -> "Can cause serious harm."
                    else -> ""
                } + (taxon.hazard.note?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** The row on the journal that opens it. */
@Composable
fun SeasonRow(month: Int, count: Int, onOpen: () -> Unit) {
    val monthName = DateFormatSymbols(Locale.getDefault()).months[month - 1]
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.art_launcher_chanterelle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp).padding(end = 8.dp),
        )
        Text(
            "$count about in $monthName",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
