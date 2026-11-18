package com.wanderwildwood.kinokocho.ui

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A photograph that comes in with a position leaves without one. */
@RunWith(RobolectricTestRunner::class)
class PhotoExifTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun geotagged(): java.io.File {
        val file = tmp.newFile("find.jpg")
        // The smallest JPEG ExifInterface will open: start and end of image.
        file.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "51/1,30/1,360/100")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "0/1,8/1,3120/100")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
            setAttribute(ExifInterface.TAG_ARTIST, "Ada Whitlock")
            setAttribute(ExifInterface.TAG_DATETIME, "2026:10:01 09:30:00")
            saveAttributes()
        }
        assertNotNull("the fixture must carry a position", ExifInterface(file).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        return file
    }

    @Test
    fun positionAndOwnerAreRemovedAndTheDateStays() {
        val file = geotagged()
        assertTrue(PhotoExif.stripLocation(file))
        val exif = ExifInterface(file)
        assertFalse(exif.getLatLong(FloatArray(2)))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_ARTIST))
        assertEquals("2026:10:01 09:30:00", exif.getAttribute(ExifInterface.TAG_DATETIME))
    }

    @Test
    fun aCleanPhotoIsLeftAlone() {
        val file = geotagged()
        PhotoExif.stripLocation(file)
        val before = file.readBytes()
        assertFalse(PhotoExif.stripLocation(file))
        assertTrue(before.contentEquals(file.readBytes()))
    }

    @Test
    fun aFileThatIsNotAPictureIsNotHarmed() {
        val file = tmp.newFile("notes.txt").apply { writeText("not a picture") }
        assertFalse(PhotoExif.stripLocation(file))
        assertEquals("not a picture", file.readText())
    }

    private fun segment(marker: Int, payload: ByteArray): ByteArray {
        val length = payload.size + 2
        return byteArrayOf(0xFF.toByte(), marker.toByte(), (length shr 8).toByte(), length.toByte()) + payload
    }

    private val xmpWithPlace = segment(
        0xE1,
        "http://ns.adobe.com/xap/1.0/\u0000<x:xmpmeta><exif:GPSLatitude>51,30.06N</exif:GPSLatitude></x:xmpmeta>"
            .toByteArray(Charsets.US_ASCII),
    )

    @Test
    fun aPlaceWrittenAsXmpIsTakenOutToo() {
        val file = tmp.newFile("xmp.jpg")
        file.writeBytes(
            byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + xmpWithPlace + byteArrayOf(0xFF.toByte(), 0xD9.toByte())
        )
        assertTrue(PhotoExif.mentionsPlace(file.readBytes()))
        assertEquals(PhotoExif.Outcome.STRIPPED, PhotoExif.strip(file))
        assertFalse(PhotoExif.mentionsPlace(file.readBytes()))
    }

    @Test
    fun onlyTheXmpSegmentsGoAndEveryOtherByteStays() {
        val start = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        val exif = segment(0xE1, "Exif\u0000\u0000 the camera's settings".toByteArray(Charsets.US_ASCII))
        val picture = byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0, 4, 1, 2, 3, 4, 0xFF.toByte(), 0xD9.toByte())
        val out = PhotoExif.withoutXmp(start + exif + xmpWithPlace + picture)
        assertTrue(out!!.contentEquals(start + exif + picture))
    }

    @Test
    fun aFileThatIsNotLaidOutAsAJpegIsNotCut() {
        assertNull(PhotoExif.withoutXmp("not a picture".toByteArray()))
        // A segment that claims to run past the end of the file.
        assertNull(PhotoExif.withoutXmp(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x7F, 0)))
    }

    @Test
    fun aFileThatCannotBeMadeCleanSaysSo() {
        val file = tmp.newFile("notes.bin").apply { writeText("not a picture") }
        assertEquals(PhotoExif.Outcome.FAILED, PhotoExif.strip(file))
        assertEquals("not a picture", file.readText())
    }
}
