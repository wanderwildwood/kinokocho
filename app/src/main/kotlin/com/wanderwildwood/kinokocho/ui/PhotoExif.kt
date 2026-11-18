package com.wanderwildwood.kinokocho.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Takes the place out of a photograph.
 *
 * The journal stores where a find was only to about a kilometer, but a camera app that
 * geotags writes the exact spot into the picture itself, and a photo picked from the
 * gallery may carry one too. Those pictures go out in a share and into a backup, so
 * the spot would leave the phone inside the file. Done once when a photograph comes in,
 * and again before one goes out, for pictures kept before this was done.
 *
 * Only the position and the tags that name a device or an owner are removed; the date
 * and the camera's settings stay, since they say nothing about where. The position can
 * sit in two places: the EXIF tags, and an XMP packet some cameras and editors write
 * beside them. Both are taken out.
 *
 * The phone can rewrite EXIF only in a JPEG, PNG or WebP. A picture in any other form
 * (HEIC, say, from a phone that shoots it) is drawn again as a plain JPEG instead, which
 * carries nothing but the picture. This fails closed: [strip] says when a file could not
 * be made clean, and a share leaves such a file out.
 *
 * It rewrites the file, so it runs off the main thread.
 */
object PhotoExif {

    enum class Outcome {
        /** Nothing in it said where; the file was not touched. */
        CLEAN,
        /** The place was in it and has been taken out. */
        STRIPPED,
        /** It could not be read or rewritten, and may still say where. */
        FAILED,
    }

    /** Removes the location and identifying tags; true if the file was changed. */
    fun stripLocation(file: File): Boolean = strip(file) == Outcome.STRIPPED

    /** One at a time: a share and a photograph coming in may reach the same file. */
    @Synchronized
    fun strip(file: File): Outcome {
        if (!file.isFile) return Outcome.FAILED
        val head = runCatching {
            file.inputStream().use { input ->
                val buf = ByteArray(16)
                var n = 0
                while (n < buf.size) {
                    val read = input.read(buf, n, buf.size - n)
                    if (read < 0) break
                    n += read
                }
                buf.copyOf(n)
            }
        }.getOrElse { return Outcome.FAILED }
        return when (kind(head)) {
            Kind.JPEG -> stripJpeg(file)
            Kind.PNG, Kind.WEBP -> stripOther(file)
            Kind.REDRAWN -> if (redraw(file)) Outcome.STRIPPED else Outcome.FAILED
            Kind.UNKNOWN -> Outcome.FAILED
        }
    }

    private fun stripJpeg(file: File): Outcome {
        val tags = stripTags(file) ?: return if (redraw(file)) Outcome.STRIPPED else Outcome.FAILED
        val bytes = runCatching { file.readBytes() }.getOrElse { return Outcome.FAILED }
        if (!mentionsPlace(bytes)) return if (tags) Outcome.STRIPPED else Outcome.CLEAN
        // A position in XMP: the packet goes, losslessly, and the rest stays as it was.
        val without = withoutXmp(bytes)
        if (without != null && !mentionsPlace(without) && replace(file, without)) return Outcome.STRIPPED
        return if (redraw(file)) Outcome.STRIPPED else Outcome.FAILED
    }

    private fun stripOther(file: File): Outcome {
        val tags = stripTags(file) ?: return if (redraw(file)) Outcome.STRIPPED else Outcome.FAILED
        val bytes = runCatching { file.readBytes() }.getOrElse { return Outcome.FAILED }
        if (!mentionsPlace(bytes)) return if (tags) Outcome.STRIPPED else Outcome.CLEAN
        return if (redraw(file)) Outcome.STRIPPED else Outcome.FAILED
    }

    /**
     * The EXIF tags taken out: true if any were there, false if none, null if it failed.
     *
     * The rewrite is done on a copy beside the file and renamed over it, so a process
     * killed halfway leaves the photograph whole: it may be the only one there is.
     */
    private fun stripTags(file: File): Boolean? {
        val present = runCatching {
            val exif = ExifInterface(file)
            TAGS.filter { exif.getAttribute(it) != null }
        }.getOrElse { return null }
        if (present.isEmpty()) return false
        val part = File(file.parentFile, ".${file.name}.exif")
        val ok = runCatching {
            file.copyTo(part, overwrite = true)
            val exif = ExifInterface(part)
            present.forEach { exif.setAttribute(it, null) }
            exif.saveAttributes()
            part.renameTo(file)
        }.getOrDefault(false)
        if (!ok) part.delete()
        return if (ok) true else null
    }

    /**
     * The picture drawn again as a JPEG with nothing else in it, in place of the file.
     *
     * The decoder turns the picture the way its EXIF says, so it still stands the right way
     * up without that tag. Kept to a size the phone can hold in memory.
     */
    private fun redraw(file: File): Boolean = runCatching {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            var sample = 1
            while (longest / sample > MAX_SIDE) sample *= 2
            decoder.setTargetSampleSize(sample)
        }
        try {
            val out = ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return@runCatching false
            replace(file, out.toByteArray())
        } finally {
            bitmap.recycle()
        }
    }.getOrDefault(false)

    /** Written beside the file and renamed over it, so a failure leaves the old one whole. */
    private fun replace(file: File, bytes: ByteArray): Boolean {
        val part = File(file.parentFile, ".${file.name}.strip")
        val ok = runCatching {
            part.writeBytes(bytes)
            part.renameTo(file)
        }.getOrDefault(false)
        if (!ok) part.delete()
        return ok
    }

    private enum class Kind { JPEG, PNG, WEBP, REDRAWN, UNKNOWN }

    /** What the file is, by its first bytes rather than by its name. */
    private fun kind(head: ByteArray): Kind {
        fun at(i: Int, vararg b: Int) = head.size >= i + b.size && b.indices.all { head[i + it] == b[it].toByte() }
        fun ascii(i: Int, text: String) = at(i, *text.map { it.code }.toIntArray())
        return when {
            at(0, 0xFF, 0xD8) -> Kind.JPEG
            at(0, 0x89, 0x50, 0x4E, 0x47) -> Kind.PNG
            ascii(0, "RIFF") && ascii(8, "WEBP") -> Kind.WEBP
            // HEIC, HEIF, AVIF and the like; and GIF and BMP, which carry no EXIF to edit.
            ascii(4, "ftyp") || ascii(0, "GIF8") || ascii(0, "BM") -> Kind.REDRAWN
            else -> Kind.UNKNOWN
        }
    }

    /** An XMP position written out as text: exif:GPSLatitude and its kin. */
    internal fun mentionsPlace(bytes: ByteArray): Boolean =
        PLACE_WORDS.any { indexOf(bytes, it) >= 0 }

    private val PLACE_WORDS = listOf("GPSLatitude", "GPSLongitude", "GPSDestLatitude", "GPSDestLongitude")
        .map { it.toByteArray(Charsets.US_ASCII) }

    private fun indexOf(bytes: ByteArray, word: ByteArray): Int {
        if (word.isEmpty() || bytes.size < word.size) return -1
        outer@ for (i in 0..bytes.size - word.size) {
            for (j in word.indices) if (bytes[i + j] != word[j]) continue@outer
            return i
        }
        return -1
    }

    /**
     * A JPEG with its XMP segments left out, every other byte as it was; null when the
     * file is not laid out as a JPEG should be.
     *
     * XMP lives in APP1 segments that open with Adobe's namespace, the main packet and any
     * extension to it. EXIF is APP1 too, and is kept.
     */
    internal fun withoutXmp(jpeg: ByteArray): ByteArray? {
        fun u8(i: Int) = jpeg[i].toInt() and 0xFF
        if (jpeg.size < 4 || u8(0) != 0xFF || u8(1) != 0xD8) return null
        val out = ByteArrayOutputStream(jpeg.size)
        out.write(jpeg, 0, 2)
        var i = 2
        while (i < jpeg.size) {
            if (u8(i) != 0xFF || i + 1 >= jpeg.size) return null
            val marker = u8(i + 1)
            when {
                // Fill before a marker.
                marker == 0xFF -> { out.write(0xFF); i += 1; continue }
                // Markers that stand alone.
                marker == 0x01 || marker in 0xD0..0xD8 -> { out.write(jpeg, i, 2); i += 2; continue }
                // The picture itself, or its end: the rest goes as it is.
                marker == 0xDA || marker == 0xD9 -> { out.write(jpeg, i, jpeg.size - i); return out.toByteArray() }
            }
            if (i + 3 >= jpeg.size) return null
            val length = (u8(i + 2) shl 8) or u8(i + 3)
            val end = i + 2 + length
            if (length < 2 || end > jpeg.size) return null
            val xmp = marker == 0xE1 && XMP_HEADERS.any { header ->
                length - 2 >= header.size && header.indices.all { jpeg[i + 4 + it] == header[it] }
            }
            if (!xmp) out.write(jpeg, i, end - i)
            i = end
        }
        return out.toByteArray()
    }

    private val XMP_HEADERS = listOf("http://ns.adobe.com/xap/1.0/\u0000", "http://ns.adobe.com/xmp/extension/\u0000")
        .map { it.toByteArray(Charsets.US_ASCII) }

    private const val MAX_SIDE = 4096
    private const val JPEG_QUALITY = 92

    // The last few by name: android.media.ExifInterface has no constants for them.
    private val TAGS = listOf(
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        "GPSHPositioningError",
        "BodySerialNumber",
        "LensSerialNumber",
        "CameraOwnerName",
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
    )
}
