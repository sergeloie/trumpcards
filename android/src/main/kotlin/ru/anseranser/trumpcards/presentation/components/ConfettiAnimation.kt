package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/confetti.json"))
    val progress by animateLottieCompositionAsState(composition)

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun GlowAnimation(
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/glow.json"))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = Int.MAX_VALUE,
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}
