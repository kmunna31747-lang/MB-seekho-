package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mbSeekhoDataStore: DataStore<Preferences> by preferencesDataStore(name = "mb_seekho_session")

class SessionPreferences(private val context: Context) {
    private val keyActiveUid = stringPreferencesKey("active_user_uid")

    val activeUidFlow: Flow<String?> = context.mbSeekhoDataStore.data.map { prefs ->
        prefs[keyActiveUid]
    }

    suspend fun saveActiveUid(uid: String) {
        context.mbSeekhoDataStore.edit { prefs ->
            prefs[keyActiveUid] = uid
        }
    }

    suspend fun clearSession() {
        context.mbSeekhoDataStore.edit { prefs ->
            prefs.remove(keyActiveUid)
        }
    }
}
