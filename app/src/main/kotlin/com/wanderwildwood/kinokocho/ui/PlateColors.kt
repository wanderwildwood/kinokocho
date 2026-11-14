package com.wanderwildwood.kinokocho.ui

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A plate in dark mode stays as it was drawn: black lines on white paper. With dark mode off
 * this changes nothing.
 */
fun Modifier.keepPlate(): Modifier = if (DarkMode.on) keepColors().background(Color.White) else this
