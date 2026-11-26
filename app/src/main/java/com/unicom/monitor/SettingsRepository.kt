package com.unicom.monitor

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val cookie: String = "",
    val mobile: String = "17651768129",
    val intervalHours: Int = 1,
    val emojiSet: String = "classic",
    val imagePath: String = "",
)

class SettingsRepository(private val context: Context) {
    private val KEY_COOKIE = stringPreferencesKey("cookie")
    private val KEY_MOBILE = stringPreferencesKey("mobile")
    private val KEY_INTERVAL = stringPreferencesKey("interval")
    private val KEY_EMOJI = stringPreferencesKey("emoji")
    private val KEY_IMAGE = stringPreferencesKey("image")

    val settings = context.dataStore.data.map { p ->
        Settings(
            cookie = p[KEY_COOKIE] ?: "",
            mobile = p[KEY_MOBILE] ?: "17651768129",
            intervalHours = (p[KEY_INTERVAL] ?: "1").toIntOrNull() ?: 1,
            emojiSet = p[KEY_EMOJI] ?: "classic",
            imagePath = p[KEY_IMAGE] ?: "",
        )
    }

    suspend fun save(s: Settings) {
        context.dataStore.edit { p ->
            p[KEY_COOKIE] = s.cookie
            p[KEY_MOBILE] = s.mobile
            p[KEY_INTERVAL] = s.intervalHours.toString()
            p[KEY_EMOJI] = s.emojiSet
            p[KEY_IMAGE] = s.imagePath
        }
    }
}
