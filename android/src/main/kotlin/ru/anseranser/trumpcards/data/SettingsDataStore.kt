package ru.anseranser.trumpcards.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.anseranser.engine.RankSet

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * What the app remembers between the games: the rank set the person picked
 * last, which is offered to them again.
 */
class SettingsDataStore(private val context: Context) {

    val rankSet: Flow<RankSet> = context.dataStore.data
        .map { prefs ->
            try {
                RankSet.valueOf(prefs[RANK_SET_KEY] ?: RankSet.SIX_TO_ACE.name)
            } catch (_: IllegalArgumentException) {
                RankSet.SIX_TO_ACE
            }
        }

    suspend fun setRankSet(rankSet: RankSet) {
        context.dataStore.edit { prefs ->
            prefs[RANK_SET_KEY] = rankSet.name
        }
    }

    companion object {
        private val RANK_SET_KEY = stringPreferencesKey("rank_set")
    }
}
