package ru.anseranser.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = CardPlayable,
    tertiary = TableGreenLight,
    background = BackgroundDark,
    surface = ScoreboardBg,
    onPrimary = BackgroundDark,
    onSecondary = BackgroundDark,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun TrumpcardsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TrumpcardsTypography,
        content = content
    )
}
