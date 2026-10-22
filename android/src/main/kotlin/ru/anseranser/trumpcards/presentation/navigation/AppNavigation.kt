package ru.anseranser.trumpcards.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.anseranser.engine.RankSet
import ru.anseranser.trumpcards.domain.GameIntent
import ru.anseranser.trumpcards.domain.GameState
import ru.anseranser.trumpcards.presentation.screens.game.GameScreen
import ru.anseranser.trumpcards.presentation.screens.gameover.GameOverScreen
import ru.anseranser.trumpcards.presentation.screens.start.StartScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    state: GameState,
    rankSet: RankSet,
    onIntent: (GameIntent) -> Unit,
) {
    NavHost(navController = navController, startDestination = "start") {
        composable("start") {
            StartScreen(
                rememberedRankSet = rankSet,
                onStartGame = { chosen ->
                    onIntent(GameIntent.StartGame(chosen))
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
                humanSeat = state.humanSeat,
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
