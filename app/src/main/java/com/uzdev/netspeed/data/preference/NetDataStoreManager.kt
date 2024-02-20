package com.uzdev.netspeed.data.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "net_info")

class NetDataStoreManager(context: Context) {

    private object PreferenceKeys {
        val pingDuration = stringPreferencesKey(name = "ping_duration")

    }


    private val dataStore = context.dataStore

    suspend fun savePingDuration(duration: String) {
        dataStore.edit {
            it[PreferenceKeys.pingDuration] = duration.ifEmpty { "5" }
        }
    }

   suspend fun getPingDuration(): Flow<String> {
        return dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
            .map { if (it[PreferenceKeys.pingDuration].isNullOrEmpty()) "5" else it[PreferenceKeys.pingDuration].toString() }
    }


}