package ru.anseranser.trumpcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.koin.android.ext.android.inject
import ru.anseranser.trumpcards.presentation.navigation.AppNavigation
import ru.anseranser.trumpcards.presentation.screens.game.GameViewModel
import ru.anseranser.trumpcards.presentation.theme.TrumpCardsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrumpCardsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val state by viewModel.state.collectAsState()
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        state = state,
                        onIntent = viewModel::onIntent,
                    )
                }
            }
        }
    }
}
