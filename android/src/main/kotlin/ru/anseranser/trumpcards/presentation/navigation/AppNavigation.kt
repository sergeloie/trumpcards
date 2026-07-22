package ru.anseranser.trumpcards.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.anseranser.trumpcards.domain.GameIntent
import ru.anseranser.trumpcards.domain.GameState
import ru.anseranser.trumpcards.presentation.screens.game.GameScreen
import ru.anseranser.trumpcards.presentation.screens.gameover.GameOverScreen
import ru.anseranser.trumpcards.presentation.screens.start.StartScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    state: GameState,
    onIntent: (GameIntent) -> Unit,
) {
    NavHost(navController = navController, startDestination = "start") {
        composable("start") {
            StartScreen(
                onStartGame = { deckSize ->
                    onIntent(GameIntent.StartGame(deckSize))
                    navController.navigate("game")
                },
            )
        }
        composable("game") {
            GameScreen(
                state = state,
                onIntent = onIntent,
                onGameFinished = {
                    navController.navigate("gameover") {
                        popUpTo("start")
                    }
                },
            )
        }
        composable("gameover") {
            GameOverScreen(
                winner = state.winner,
                onPlayAgain = {
                    onIntent(GameIntent.NewGame)
                    navController.navigate("start") {
                        popUpTo(0)
                    }
                },
                onGoToMenu = {
                    onIntent(GameIntent.GoToMenu)
                    navController.navigate("start") {
                        popUpTo(0)
                    }
                },
            )
        }
    }
}
