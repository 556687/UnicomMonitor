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
    val lastGeneral: String = "",
    val lastFree: String = "",
    val todayGeneral: String = "",
    val todayFree: String = "",
    val todayDate: String = "",
    val keepTodayOnReset: Boolean = true,
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
    private val KEY_LAST_G = stringPreferencesKey("last_general")
    private val KEY_LAST_F = stringPreferencesKey("last_free")
    private val KEY_TODAY_G = stringPreferencesKey("today_general")
    private val KEY_TODAY_F = stringPreferencesKey("today_free")
    private val KEY_TODAY_DATE = stringPreferencesKey("today_date")
    private val KEY_KEEP_TODAY = stringPreferencesKey("keep_today_reset")

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
            lastGeneral = p[KEY_LAST_G] ?: "",
            lastFree = p[KEY_LAST_F] ?: "",
            todayGeneral = p[KEY_TODAY_G] ?: "",
            todayFree = p[KEY_TODAY_F] ?: "",
            todayDate = p[KEY_TODAY_DATE] ?: "",
            keepTodayOnReset = p[KEY_KEEP_TODAY] != "false",
        )
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
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
            p[KEY_LAST_G] = s.lastGeneral
            p[KEY_LAST_F] = s.lastFree
            p[KEY_TODAY_G] = s.todayGeneral
            p[KEY_TODAY_F] = s.todayFree
            p[KEY_TODAY_DATE] = s.todayDate
            p[KEY_KEEP_TODAY] = s.keepTodayOnReset.toString()
        }
    }
}
