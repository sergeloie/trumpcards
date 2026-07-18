package ru.anseranser.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.anseranser.android.ui.screens.GameOverScreen
import ru.anseranser.android.ui.screens.GameScreen
import ru.anseranser.android.ui.screens.MainMenuScreen
import ru.anseranser.android.ui.theme.TrumpcardsTheme
import ru.anseranser.model.Card

private const val TAG = "Trumpcards"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            TrumpcardsTheme {
                TrumpcardsNavHost()
            }
        }
    }
}

@Composable
fun TrumpcardsNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MainMenuScreen(
                onStartGame = { navController.navigate("game") }
            )
        }
        composable("game") {
            GameScreen(
                onGameOver = { winnerSuit ->
                    navController.navigate("gameover/${winnerSuit?.name ?: ""}") {
                        popUpTo("menu")
                    }
                }
            )
        }
        composable("gameover/{winner}") { backStackEntry ->
            val winnerName = backStackEntry.arguments?.getString("winner")
            val winnerSuit = winnerName?.let {
                try { Card.Suit.valueOf(it) } catch (_: Exception) { null }
            }
            GameOverScreen(
                winnerSuit = winnerSuit,
                onPlayAgain = {
                    navController.navigate("game") {
                        popUpTo("menu")
                    }
                },
                onMainMenu = {
                    navController.navigate("menu") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
