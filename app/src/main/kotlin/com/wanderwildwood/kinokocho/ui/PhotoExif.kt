package com.wanderwildwood.kinokocho.ui

import android.media.ExifInterface
import java.io.File

/**
 * Takes the place out of a photograph.
 *
 * The journal stores where a find was only to about a kilometer, but a camera app that
 * geotags writes the exact spot into the picture itself, and a photo picked from the
 * gallery may carry one too. Those pictures go out in a share and into a backup, so
 * the spot would leave the phone inside the JPEG. Done once when a photograph comes in,
 * and again before one goes out, for pictures kept before this was done.
 *
 * Only the position and the tags that name a device or an owner are removed; the date
 * and the camera's settings stay, since they say nothing about where.
 */
object PhotoExif {

    /** Removes the location and identifying tags; true if the file was changed. */
    fun stripLocation(file: File): Boolean = runCatching {
        if (!file.isFile) return false
        val exif = ExifInterface(file)
        val present = TAGS.filter { exif.getAttribute(it) != null }
        if (present.isEmpty()) return false
        present.forEach { exif.setAttribute(it, null) }
        exif.saveAttributes()
        true
    }.getOrDefault(false)

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
