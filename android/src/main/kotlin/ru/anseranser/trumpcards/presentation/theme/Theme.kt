package ru.anseranser.trumpcards.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = GoldDark,
    tertiary = Green800,
    background = TableGreen,
    surface = TableGreenLight,
    onPrimary = TableGreen,
    onSecondary = TableGreen,
    onTertiary = CardWhite,
    onBackground = CardWhite,
    onSurface = CardWhite,
)

@Composable
fun TrumpCardsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
