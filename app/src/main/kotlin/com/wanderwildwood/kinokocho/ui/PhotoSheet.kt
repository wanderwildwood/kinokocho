package com.wanderwildwood.kinokocho.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import java.io.File
import java.util.UUID

/**
 * What a picture is of, and why the app asks for that one.
 *
 * The slots are the teaching part. A single photograph of a cap from above is what
 * almost everyone takes and is worth almost nothing to whoever is asked later; the
 * underside and the base of the stem are what get asked for, every time. An empty
 * "base of the stem" slot is the app saying, without saying it, that the specimen was
 * probably not dug up.
 */
enum class PhotoSlot(val id: String, val label: String, val why: String) {
    WHOLE("whole", "The whole thing", "In proportion, so size and habit read."),
    CAP("cap", "The cap, from above", "Surface, colour and how the edge sits."),
    UNDERSIDE("underside", "Underneath", "Gills, pores or teeth, and how they meet the stem."),
    STIPE_BASE("stipe_base", "The base of the stem",
        "Dug up, not cut. A volva or a bulb lives here and it is the commonest thing missed."),
    IN_SITU("in_situ", "Where it was growing", "Substrate and what is around it."),
    SPORE_PRINT("spore_print", "The spore print", "At home, on paper, the next morning."),
}

/**
 * The photographs for one find.
 *
 * Capture goes out to whatever camera app is on the phone rather than opening one
 * here. That is not laziness: an app that declares CAMERA must then hold the
 * permission, and this app's manifest asks for nothing. Handing an intent to the
 * system camera needs no permission at all, so the picture arrives and the manifest
 * stays empty. It is also the camera the person already knows.
 */
@Composable
fun PhotoSheet(
    filled: Set<String>,
    onCaptured: (slot: String, fileName: String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<PhotoSlot, String>?>(null) }

    var choosing by remember { mutableStateOf<PhotoSlot?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val (slot, fileName) = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (ok) {
            onCaptured(slot.id, fileName)
        } else {
            // Cancelled: take the empty file back out rather than leaving a nought-byte
            // photograph in the directory pretending to be a picture.
            File(photoDir(context), fileName).delete()
        }
    }

    /*
     * Pictures taken outside the app.
     *
     * People photograph a mushroom before they think to open a journal about it, and
     * telling them the picture they already have is no good would be absurd. The system
     * photo picker needs no permission at all — it hands back one image the person
     * chose and nothing else, which is a better bargain than read-my-whole-gallery.
     *
     * The file is copied into the app's own folder rather than referenced where it sits:
     * a content:// URI is a loan that expires, and a journal entry pointing at a
     * photograph the app can no longer open is worse than one with no photograph.
     */
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val slot = choosing
        choosing = null
        if (uri != null && slot != null) {
            val fileName = "${UUID.randomUUID()}.jpg"
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    File(photoDir(context), fileName).outputStream().use(input::copyTo)
                } != null
            }.getOrDefault(false)
            if (copied) onCaptured(slot.id, fileName)
        }
    }

    // Scrolls. Six slots and the Done row fit a 480x800 panel with nothing to spare, and
    // the About dialog has already demonstrated what happens to the last row when one of
    // these outgrows the screen: it lands in the system's gesture strip and stops being
    // tappable at all. A seventh slot should be a layout that gets longer, not a control
    // that quietly disappears.
    Column(
        Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        TextMMD(
            "Photographs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
        TextMMD(
            "Whoever you ask will want the underside and the base. The rest is optional.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )

        PhotoSlot.entries.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { slot ->
                    SlotTile(
                        slot = slot,
                        taken = slot.id in filled,
                        modifier = Modifier.weight(1f),
                        onClick = { choosing = slot },
                    )
                }
                if (pair.size == 1) Column(Modifier.weight(1f)) {}
            }
        }

        // Camera or gallery, asked once the slot is known — the slot is the useful
        // question and "where from" is an afterthought.
        choosing?.let { slot ->
            Dialog(onDismissRequest = { choosing = null }) {
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                        .padding(20.dp),
                ) {
                    TextMMD(
                        slot.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextMMD(
                        slot.why,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                    )
                    ButtonMMD(
                        onClick = {
                            val fileName = "${UUID.randomUUID()}.jpg"
                            val file = File(photoDir(context), fileName)
                            pending = slot to fileName
                            choosing = null
                            camera.launch(uriFor(context, file))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { TextMMD("Take one now") }
                    OutlinedButtonMMD(
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) { TextMMD("Choose one already on the phone") }
                    OutlinedButtonMMD(
                        onClick = { choosing = null },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) { TextMMD("Back") }
                }
            }
        }

        TextMMD(
            "Done",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                .clickable(onClick = onClose)
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun SlotTile(
    slot: PhotoSlot,
    taken: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .border(
                BorderStroke(if (taken) 3.dp else 1.dp, MaterialTheme.colorScheme.onSurface),
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The base-of-stem slot carries the drawing of a sac volva, because that is
        // the thing the photograph is for.
        CharacterArt.of("stipe_base", "sac_volva")
            ?.takeIf { slot == PhotoSlot.STIPE_BASE }
            ?.let {
                Icon(
                    painter = painterResource(it.drawable),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }
        TextMMD(
            slot.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (taken) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        TextMMD(
            if (taken) "taken" else slot.why,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * Where photographs live: inside the app's own files, never the shared gallery.
 *
 * Birding put its recordings in the phone's shared Music folder and they turned up in
 * whatever plays music. A picture of a mushroom in someone's camera roll is a smaller
 * annoyance than that but the same mistake, and where a person forages is not a thing
 * to scatter about the phone.
 */
fun photoDir(context: Context): File =
    File(context.filesDir, "photos").apply { mkdirs() }

private fun uriFor(context: Context, file: File) =
    FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
