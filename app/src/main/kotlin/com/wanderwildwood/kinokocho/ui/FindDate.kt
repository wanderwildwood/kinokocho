package com.wanderwildwood.kinokocho.ui

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * When a find was made, written the way the phone writes dates: its language, its 12- or
 * 24-hour clock. "Nov 26, 2026, 8:10 AM" in US English, the same as the other apps.
 */
object FindDate {

    fun pattern(locale: Locale, twentyFourHour: Boolean): String =
        DateFormat.getBestDateTimePattern(locale, if (twentyFourHour) "yMMMdHm" else "yMMMdhma")

    fun format(millis: Long, locale: Locale, twentyFourHour: Boolean): String =
        SimpleDateFormat(pattern(locale, twentyFourHour), locale).format(Date(millis))

    fun shown(context: Context, millis: Long): String =
        format(millis, locale(context.resources), DateFormat.is24HourFormat(context))

    /**
     * The date as a search matches it: what the row shows, plus the month spelled out,
     * so "September" still finds a find shown as "Sep 3".
     */
    fun searchable(millis: Long, locale: Locale, twentyFourHour: Boolean): String =
        format(millis, locale, twentyFourHour) + " " +
            SimpleDateFormat("LLLL yyyy", locale).format(Date(millis))

    fun locale(resources: Resources): Locale = resources.configuration.locales[0]
}
