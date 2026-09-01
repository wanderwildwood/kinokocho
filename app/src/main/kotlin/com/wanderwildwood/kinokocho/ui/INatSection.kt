package com.wanderwildwood.kinokocho.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.wanderwildwood.kinokocho.JournalViewModel
import com.wanderwildwood.kinokocho.net.INatAccount
import com.wanderwildwood.kinokocho.net.INatConfig
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Publishing a find, at the bottom of the entry it belongs to.
 *
 * Here and nowhere else — not on the question screen, not on the journal, not as
 * something that happens to a batch. Publishing is a home action taken deliberately
 * about one mushroom, which is the rule PRIVACY.md set before any of it was built, and
 * the surest way to keep it is to give it exactly one door.
 *
 * The heading is drawn by the caller, so that this file needs no copy of EntryScreen's
 * file-private `Section` - which CandidateScreen also has one of, and a third would be
 * two too many.
 */
@Composable
fun INatSection(vm: JournalViewModel, draft: JournalViewModel.Draft) {
    val context = LocalContext.current
    val state by vm.inatState.collectAsState()

    if (!INatConfig.configured) {
        // Says what it cannot do rather than showing a button that fails when pressed.
        // Self-eliminating: a build with an application id never draws this.
        Text(
            "This build has no iNaturalist application id, so it cannot publish.",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }

    val published = draft.inat.uuid != null

    when {
        !vm.inat.signedIn -> {
            Text(
                "Publishing a find puts it, its photographs and its characters on a " +
                    "public website, under your own account.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButtonMMD(
                onClick = { openOrSay(context, vm, vm.signInIntent(), "sign in") },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { Text("Sign in to iNaturalist") }
        }

        published -> Published(vm, draft, context)

        else -> Publish(vm, draft)
    }

    // Whatever just happened, said once, in the place it happened. Cleared when the
    // reader opens a different find - see the LaunchedEffect in EntryScreen.
    when (val s = state) {
        is JournalViewModel.INatState.Idle -> Unit
        is JournalViewModel.INatState.Working ->
            Text(s.said, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        is JournalViewModel.INatState.Said -> Column(Modifier.padding(top = 8.dp)) {
            Text(s.said, style = MaterialTheme.typography.bodySmall)
        }
    }

    /*
     * Who this phone is signed in as, and the way out.
     *
     * Outside the branch, because it is a fact about the account rather than about this
     * find - it was reachable only from a find that had not been published yet, so
     * somebody who had published everything could not find out whose account they were
     * posting to. Last, because STYLE.md puts the destructive row after everything that
     * only adjusts.
     */
    if (vm.inat.signedIn) {
        vm.inat.login?.let {
            Text(
                "Signed in as $it",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        OutlinedButtonMMD(
            onClick = vm::signOutOfINat,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) { Text("Sign out") }
    }

}

/** A find not yet published: what will be sent, and the button that sends it. */
@Composable
private fun Publish(vm: JournalViewModel, draft: JournalViewModel.Draft) {
    // Keyed on the find, not remembered loosely. See the note on `armed` below; the
    // setting itself is global, but re-reading it per find also means a change made
    // while looking at another one is not shown stale here.
    var geoprivacy by remember(draft.uuid) { mutableStateOf(vm.inat.geoprivacy) }

    /*
     * A find with no coordinates, which is the ordinary case rather than the odd one.
     *
     * Typing the place by hand is this app's default and asks for no permission, so most
     * entries have words and no numbers. Showing "Location: obscured" over one of those
     * would be the app describing a setting that changes nothing about what it is
     * actually going to send.
     *
     * And it is worth saying what it costs, because it is not nothing and it is not
     * obvious: iNaturalist tests for a location before anything else when deciding
     * whether an observation can be verified, so one without coordinates stays Casual
     * for ever - left out of exports, and largely passed over by the people who might
     * have named it. Somebody should be able to find that out here rather than in three
     * weeks of silence.
     */
    if (draft.latitude == null || draft.longitude == null) {
        Text(
            "No coordinates on this find",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "It will go up with the place in your own words and no map pin. iNaturalist " +
                "cannot verify a find it cannot place, so it will sit outside the lists " +
                "most identifiers work through. \"Add roughly where I am\", further up, " +
                "fixes that.",
            style = MaterialTheme.typography.bodySmall,
        )
    } else {

    /*
     * The one setting worth putting in front of somebody before they press publish.
     *
     * A note under it rather than a bare label, which STYLE.md allows for exactly this
     * case: a setting whose meaning no label can carry. "Obscured" sounds like the
     * coordinates are not sent, and they are - the fuzzing is what the public sees.
     * Somebody deciding whether to publish a foraging patch deserves the true version
     * of that sentence rather than the reassuring one.
     */
    Text(
        when (geoprivacy) {
            INatAccount.PRIVATE -> "Location: hidden"
            INatAccount.OBSCURED -> "Location: obscured"
            else -> "Location: exact"
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        when (geoprivacy) {
            INatAccount.PRIVATE ->
                "iNaturalist is told where it was and shows nobody, including on the map. " +
                    "Nobody can check the record against the place."
            INatAccount.OBSCURED ->
                "iNaturalist is told where it was and shows the public a random point " +
                    "within about twenty kilometres. The place you typed is kept, not shown."
            else ->
                "The map pin is where you found it, for anyone."
        },
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedButtonMMD(
        onClick = {
            geoprivacy = INatAccount.next(geoprivacy)
            vm.inat.geoprivacy = geoprivacy
        },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) { Text("Change") }
    }

    /*
     * Arm and confirm, per STYLE.md.
     *
     * Publishing is not destructive in the way deleting is - it can be undone on
     * iNaturalist - but it is outward-facing and it cannot be taken back from anybody
     * who already read it, which is the same reason for asking. The row disarms itself
     * after four seconds so a stray tap does not leave a live trigger.
     */
    /*
     * ⚠ Keyed on the find. Without the key this is the photograph bug again, which this
     * repo has already fixed once ("The armed photograph never put its own question
     * down"): Compose keeps the state in the slot, not with the data, so arming Publish
     * on one find and then opening another within four seconds left the second one armed
     * — and a single tap published a mushroom nobody had been asked about. Four seconds
     * is a narrow window and it is somebody's observation going public.
     */
    var armed by remember(draft.uuid) { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4000)
            armed = false
        }
    }
    OutlinedButtonMMD(
        onClick = {
            if (armed) {
                armed = false
                vm.publish(draft)
            } else {
                armed = true
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Text(if (armed) "Publish — this is public; tap again" else "Publish to iNaturalist")
    }

}

/** A find already on iNaturalist: where it is, and what has been said about it. */
@Composable
private fun Published(
    vm: JournalViewModel,
    draft: JournalViewModel.Draft,
    context: android.content.Context,
) {
    val url = "https://www.inaturalist.org/observations/${draft.inat.uuid}"

    Text(
        draft.inat.pushedAt?.let { "Published ${dateOnly(it)}" } ?: "Published",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
    )

    /*
     * What the community called it, and when that was read.
     *
     * The date is not fussiness. An identification on iNaturalist can be revised, and a
     * name with no date on it is a name of unknown age presented as current - which in
     * an app about mushrooms is the wrong way round.
     */
    val name = draft.inat.taxonName
    if (name != null) {
        Text("iNaturalist says: $name", style = MaterialTheme.typography.bodySmall)
        draft.inat.identificationFetchedAt?.let {
            Text("Read ${dateOnly(it)}", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Text(
            "Nobody has named it yet. Fungi wait longer than most things there.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    /*
     * The way to finish a push that stopped halfway.
     *
     * Without this there was none. A find is written down as published before any
     * photograph goes up - deliberately, so an interruption leaves something findable
     * rather than an orphan - which means a connection dying after two of five pictures
     * flipped this section to "published" and took the button away with it. The find was
     * on iNaturalist missing most of its photographs and nothing in the app would send
     * the rest.
     *
     * Publishing again is the whole repair, and it is safe: iNaturalist matches the
     * observation on its uuid and each photograph on its own, so what is already there
     * is updated rather than duplicated.
     */
    val unsent = draft.photos.count { it.inatPhotoId == null }
    if (unsent > 0) {
        Text(
            "$unsent photograph${if (unsent == 1) "" else "s"} did not go up.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedButtonMMD(
            onClick = { vm.publish(draft) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) { Text("Send the rest") }
    }

    OutlinedButtonMMD(
        onClick = {
            openOrSay(context, vm, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "open that")
        },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) { Text("Open on iNaturalist") }

    // Asks about every find already published, not only this one - one call for the lot
    // rather than making somebody open twenty entries - so the label does not promise
    // to check just this mushroom.
    OutlinedButtonMMD(
        onClick = vm::checkForNames,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) { Text("Check what has been named") }
}

/**
 * Hands something to a browser, or says there is not one.
 *
 * `startActivity` on a VIEW intent throws [android.content.ActivityNotFoundException]
 * when nothing on the phone handles it, and an uncaught one closes the app. A Kompakt
 * ships with a browser so this ought never to fire — but "ought never" is doing a lot of
 * work in a sentence about a device somebody has been stripping apps off, and the cost of
 * being wrong is the journal disappearing mid-sentence rather than a line of text.
 *
 * Sign-in genuinely cannot proceed without a browser, and that is the honest thing to
 * say: this app will not draw the login form itself, which is the entire reason it never
 * sees a password.
 */
private fun openOrSay(
    context: android.content.Context,
    vm: JournalViewModel,
    intent: Intent,
    doing: String,
) {
    runCatching { context.startActivity(intent) }.onFailure {
        vm.sayINat("There is no browser on this phone to $doing with.")
    }
}

private fun dateOnly(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(millis))
