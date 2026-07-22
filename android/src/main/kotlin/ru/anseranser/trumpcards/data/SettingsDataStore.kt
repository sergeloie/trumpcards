package ru.anseranser.trumpcards.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.anseranser.model.DeckSize

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    val defaultDeckSize: Flow<DeckSize> = context.dataStore.data
        .map { prefs ->
            try {
                DeckSize.valueOf(prefs[DECK_SIZE_KEY] ?: DeckSize.THIRTY_SIX.name)
            } catch (_: Exception) {
                DeckSize.THIRTY_SIX
            }
        }

    suspend fun setDeckSize(size: DeckSize) {
        context.dataStore.edit { prefs ->
            prefs[DECK_SIZE_KEY] = size.name
        }
    }

    companion object {
        private val DECK_SIZE_KEY = stringPreferencesKey("deck_size")
    }
}
