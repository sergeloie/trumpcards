package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ru.anseranser.model.Card
import ru.anseranser.trumpcards.util.CardImageMapper

@Composable
fun CardView(
    card: Card,
    onClick: (() -> Unit)? = null,
    isPlayable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val assetPath = CardImageMapper.facePath(card)
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/$assetPath")
            .crossfade(true)
            .build(),
    )

    Box(
        modifier = modifier
            .aspectRatio(2.5f / 3.5f)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .then(
                if (isPlayable) {
                    Modifier.border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
    ) {
        Image(
            painter = painter,
            contentDescription = "${card.rank()} of ${card.suit()}",
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
