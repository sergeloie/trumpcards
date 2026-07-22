package ru.anseranser.trumpcards.presentation.screens.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import ru.anseranser.model.DeckSize
import ru.anseranser.trumpcards.util.CardImageMapper

@Composable
fun StartScreen(
    onStartGame: (DeckSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/${CardImageMapper.backgroundPath()}")
            .crossfade(true)
            .build(),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = bgPainter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Trump Cards",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Выберите колоду",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onStartGame(DeckSize.THIRTY_SIX) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "36 карт",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onStartGame(DeckSize.FIFTY_TWO) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
            ),
        ) {
            Text(
                text = "52 карты",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
