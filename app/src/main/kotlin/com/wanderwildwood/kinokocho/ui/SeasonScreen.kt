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
import com.wanderwildwood.kinokocho.key.Prevalence
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
 *
 * **And the sought-after ones are listed too**, which is not a retreat from that. The
 * top of this page used to hold nothing but poisons, and a page that can only warn is a
 * page nobody opens in September — the warning is worth most when it is on the list
 * somebody was going to read anyway. "Sought after" says people go looking for it. It
 * is not this app saying anything is safe to eat, and the line under the heading says
 * so rather than leaving it to be inferred.
 *
 * Short on purpose. It was every taxon recorded in the month, which for September is
 * most of the pack — a calendar of everything is a calendar of nothing. What is left is
 * the ones a walker actually meets, except that anything lethal is here however rarely
 * it turns up. Dropping a death cap for being uncommon is not a trade this app makes.
 */
@Composable
fun SeasonScreen(
    taxa: List<Taxon>,
    month: Int,
    onClose: () -> Unit,
    onOpen: (String) -> Unit = {},
) {
    val inSeason = taxa.filter { it.seasonMonths.isEmpty() || month in it.seasonMonths }

    val worthKnowing = worthKnowing(inSeason)
    val rest = (inSeason - worthKnowing.toSet()).sortedBy { it.scientificName }
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

        if (worthKnowing.isNotEmpty()) {
            item {
                HorizontalDividerMMD(Modifier.padding(top = 10.dp))
                Text(
                    "Worth knowing this month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                Text(
                    "The ones people go looking for and the ones that hurt people, " +
                        "which are often out at the same time. Sought after means people " +
                        "look for it, not that this app has told you anything is safe.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(worthKnowing.size) { i -> TaxonLine(worthKnowing[i], onOpen) }
        }

        item {
            HorizontalDividerMMD(Modifier.padding(top = 10.dp))
            Text(
                "Also about",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }
        items(rest.size) { i -> TaxonLine(rest[i], onOpen) }

        item {
            OutlinedButtonMMD(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
            ) { Text("Close") }
        }
    }
}

/**
 * One taxon, named, with the one thing worth saying about it here.
 *
 * Still no edibility and still no ranking. A dangerous one carries its severity and its
 * note, because that is information a person needs before they are holding it rather
 * than after; a sought-after one is marked as sought after and nothing more.
 */
@Composable
private fun TaxonLine(taxon: Taxon, onOpen: (String) -> Unit) {
    val danger = taxon.hazard.severity.alwaysShow
    // Tapping a name opens the same page a shortlist opens, with nothing laid against
    // it. Reading about a mushroom before finding one is how anybody learns which ones
    // to look at, and the page was already written.
    Column(
        Modifier.fillMaxWidth()
            .clickable { onOpen(taxon.id) }
            .padding(vertical = 6.dp),
    ) {
        Text(
            taxon.commonName?.let { "${taxon.scientificName} — $it" }
                ?: taxon.scientificName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (danger) FontWeight.Bold else FontWeight.Normal,
        )
        when {
            danger -> Text(
                when (taxon.hazard.severity) {
                    Hazard.Severity.LETHAL -> "Can kill."
                    Hazard.Severity.SEVERE -> "Can cause serious harm."
                    else -> ""
                } + (taxon.hazard.note?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
            taxon.sought -> Text(
                "Sought after." + when (taxon.hazard.severity) {
                    Hazard.Severity.GI -> " Also makes some people ill."
                    Hazard.Severity.INTOXICATION -> " Also intoxicating."
                    Hazard.Severity.UNKNOWN -> " Nothing documented either way."
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * The ones worth naming at the top, out of everything in season.
 *
 * Lethal whatever its prevalence; otherwise the ones a walker actually meets, whether
 * they are looked for or avoided. Ordered by how badly it would go to get it wrong, then
 * by how likely you are to meet it.
 *
 * A function rather than four lines inside the composable, because the rule is the part
 * worth being sure of: that a death cap cannot be dropped for being uncommon, and that
 * the list stays a list rather than becoming the pack again.
 */
fun worthKnowing(inSeason: List<Taxon>): List<Taxon> = inSeason
    .filter {
        it.hazard.severity == Hazard.Severity.LETHAL ||
            (it.prevalence != Prevalence.UNCOMMON &&
                (it.hazard.severity.alwaysShow || it.sought))
    }
    .sortedWith(
        compareByDescending<Taxon> { it.hazard.severity.ordinal }
            .thenBy { it.prevalence.ordinal }
            .thenBy { it.scientificName }
    )
    .take(MOST)

/**
 * How many the month view will name at the top.
 *
 * A number rather than a scroll. Ten is about what a person carries out of the door.
 */
private const val MOST = 10

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
