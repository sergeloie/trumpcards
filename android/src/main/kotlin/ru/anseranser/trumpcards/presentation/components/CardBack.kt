package ru.anseranser.trumpcards.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ru.anseranser.trumpcards.util.CardImageMapper

@Composable
fun CardBack(
    cardCount: Int,
    modifier: Modifier = Modifier,
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/${CardImageMapper.backPath()}")
            .crossfade(true)
            .build(),
    )

    Box(
        modifier = modifier
            .aspectRatio(2.5f / 3.5f)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A237E)),
    ) {
        Image(
            painter = painter,
            contentDescription = "Card back",
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = "$cardCount",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black,
                    blurRadius = 4f,
                ),
            ),
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
