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
    val bgPath: String = "",
    val avatarPath: String = "",
    val textColor: String = "black",
    val customInterval: String = "",
    val customText: String = "",
)

class SettingsRepository(private val context: Context) {
    private val KEY_COOKIE = stringPreferencesKey("cookie")
    private val KEY_MOBILE = stringPreferencesKey("mobile")
    private val KEY_INTERVAL = stringPreferencesKey("interval")
    private val KEY_EMOJI = stringPreferencesKey("emoji")
    private val KEY_IMAGE = stringPreferencesKey("image")
    private val KEY_BG = stringPreferencesKey("bg")
    private val KEY_AVATAR = stringPreferencesKey("avatar")
    private val KEY_TEXTCOLOR = stringPreferencesKey("textcolor")
    private val KEY_INTERVAL_CUSTOM = stringPreferencesKey("interval_custom")
    private val KEY_CUSTOM_TEXT = stringPreferencesKey("custom_text")

    val settings = context.dataStore.data.map { p ->
        Settings(
            cookie = p[KEY_COOKIE] ?: "",
            mobile = p[KEY_MOBILE] ?: "17651768129",
            intervalHours = (p[KEY_INTERVAL] ?: "1").toIntOrNull() ?: 1,
            emojiSet = p[KEY_EMOJI] ?: "classic",
            imagePath = p[KEY_IMAGE] ?: "",
            bgPath = p[KEY_BG] ?: "",
            avatarPath = p[KEY_AVATAR] ?: "",
            textColor = p[KEY_TEXTCOLOR] ?: "black",
            customInterval = p[KEY_INTERVAL_CUSTOM] ?: "",
            customText = p[KEY_CUSTOM_TEXT] ?: "",
        )
    }

    suspend fun save(s: Settings) {
        context.dataStore.edit { p ->
            p[KEY_COOKIE] = s.cookie
            p[KEY_MOBILE] = s.mobile
            p[KEY_INTERVAL] = s.intervalHours.toString()
            p[KEY_EMOJI] = s.emojiSet
            p[KEY_IMAGE] = s.imagePath
            p[KEY_BG] = s.bgPath
            p[KEY_AVATAR] = s.avatarPath
            p[KEY_TEXTCOLOR] = s.textColor
            p[KEY_INTERVAL_CUSTOM] = s.customInterval
            p[KEY_CUSTOM_TEXT] = s.customText
        }
    }
}
