package ru.anseranser.trumpcards.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_history")
data class GameHistoryEntity(
    val id: Long = 0,
    val winner: String,
    val deckSize: String,
    val roundsPlayed: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

@Dao
interface GameHistoryDao {
    @Query("SELECT * FROM game_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentGames(): Flow<List<GameHistoryEntity>>

    @Insert
    suspend fun insert(game: GameHistoryEntity)
}
