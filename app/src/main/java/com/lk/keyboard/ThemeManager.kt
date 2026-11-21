package com.lk.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lkKeyboardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lk_keyboard_prefs"
)

enum class KeyboardThemePreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
) {
    CYBER_CYAN(
        id = "cyber_cyan",
        displayName = "Cyber Cyan",
        subtitle = "Obsidian base with neon cyan & purple accents",
    ),
    NEBULA_VIOLET(
        id = "nebula_violet",
        displayName = "Nebula Violet",
        subtitle = "Deep void with electric violet & cobalt glow",
    ),
    COBALT_MATRIX(
        id = "cobalt_matrix",
        displayName = "Cobalt Matrix",
        subtitle = "Midnight slate with crisp azure & teal highlights",
    ),
    MIDNIGHT_MINIMAL(
        id = "midnight_minimal",
        displayName = "Midnight OLED",
        subtitle = "Pure black surface with subtle ice-blue trim",
    ),
}

data class KeyboardPalette(
    val preset: KeyboardThemePreset,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val toolbarSurface: Color,
    val keyFill: Color,
    val keyPressedFill: Color,
    val functionKeyFill: Color,
    val spaceKeyFill: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryAccent: Color,
    val keyText: Color,
    val keySecondaryText: Color,
    val keyBorder: Color,
    val keyGlowBorder: Color,
    val enterButtonStart: Color,
    val enterButtonEnd: Color,
    val suggestionChipFill: Color,
)

data class KeyboardSettings(
    val themePreset: KeyboardThemePreset = KeyboardThemePreset.CYBER_CYAN,
    val showNumberRow: Boolean = true,
    val showKeyPopup: Boolean = true,
    val neonGlowBorders: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val vibrationStrengthMs: Int = 18,
    val autoCapitalize: Boolean = true,
    val predictiveSuggestions: Boolean = true,
    val incognitoMode: Boolean = false,
    val romanUrduAutoConvert: Boolean = false,
    val activeLanguageCode: String = KeyboardLanguage.ENGLISH.code,
    val enabledLanguageCodes: Set<String> = KeyboardLanguage.entries.map { it.code }.toSet(),
    val customTransliterations: Map<String, String> = emptyMap(),
)

object ThemeManager {
    fun paletteFor(preset: KeyboardThemePreset): KeyboardPalette = when (preset) {
        KeyboardThemePreset.CYBER_CYAN -> KeyboardPalette(
            preset = preset,
            backgroundTop = Color(0xFF0A0E1A),
            backgroundBottom = Color(0xFF060811),
            toolbarSurface = Color(0xFF0F1526),
            keyFill = Color(0xFF1A2238),
            keyPressedFill = Color(0xFF28375E),
            functionKeyFill = Color(0xFF131A2D),
            spaceKeyFill = Color(0xFF1D2742),
            primaryAccent = Color(0xFF00E5FF),
            secondaryAccent = Color(0xFFA855F7),
            tertiaryAccent = Color(0xFF3B82F6),
            keyText = Color(0xFFF4F7FB),
            keySecondaryText = Color(0xFF8FA3C8),
            keyBorder = Color(0xFF2A3656),
            keyGlowBorder = Color(0xFF00E5FF),
            enterButtonStart = Color(0xFF00C6FF),
            enterButtonEnd = Color(0xFF8B5CF6),
            suggestionChipFill = Color(0xFF172038),
        )

        KeyboardThemePreset.NEBULA_VIOLET -> KeyboardPalette(
            preset = preset,
            backgroundTop = Color(0xFF0E0A1B),
            backgroundBottom = Color(0xFF070510),
            toolbarSurface = Color(0xFF151029),
            keyFill = Color(0xFF211A3A),
            keyPressedFill = Color(0xFF362A60),
            functionKeyFill = Color(0xFF18122E),
            spaceKeyFill = Color(0xFF261E44),
            primaryAccent = Color(0xFFC084FC),
            secondaryAccent = Color(0xFF38BDF8),
            tertiaryAccent = Color(0xFF818CF8),
            keyText = Color(0xFFF5F3FF),
            keySecondaryText = Color(0xFFA79BC8),
            keyBorder = Color(0xFF342A59),
            keyGlowBorder = Color(0xFFC084FC),
            enterButtonStart = Color(0xFF9333EA),
            enterButtonEnd = Color(0xFF3B82F6),
            suggestionChipFill = Color(0xFF211A3D),
        )

        KeyboardThemePreset.COBALT_MATRIX -> KeyboardPalette(
            preset = preset,
            backgroundTop = Color(0xFF07111E),
            backgroundBottom = Color(0xFF040912),
            toolbarSurface = Color(0xFF0C1A2D),
            keyFill = Color(0xFF14273F),
            keyPressedFill = Color(0xFF1F3C61),
            functionKeyFill = Color(0xFF0E1D31),
            spaceKeyFill = Color(0xFF182F4D),
            primaryAccent = Color(0xFF38BDF8),
            secondaryAccent = Color(0xFF2DD4BF),
            tertiaryAccent = Color(0xFF6366F1),
            keyText = Color(0xFFF0F9FF),
            keySecondaryText = Color(0xFF8DB3D6),
            keyBorder = Color(0xFF213C5E),
            keyGlowBorder = Color(0xFF38BDF8),
            enterButtonStart = Color(0xFF0284C7),
            enterButtonEnd = Color(0xFF14B8A6),
            suggestionChipFill = Color(0xFF13263E),
        )

        KeyboardThemePreset.MIDNIGHT_MINIMAL -> KeyboardPalette(
            preset = preset,
            backgroundTop = Color(0xFF040508),
            backgroundBottom = Color(0xFF000000),
            toolbarSurface = Color(0xFF0B0D13),
            keyFill = Color(0xFF161922),
            keyPressedFill = Color(0xFF272D3D),
            functionKeyFill = Color(0xFF10131B),
            spaceKeyFill = Color(0xFF191D29),
            primaryAccent = Color(0xFF7DD3FC),
            secondaryAccent = Color(0xFFA78BFA),
            tertiaryAccent = Color(0xFF60A5FA),
            keyText = Color(0xFFE2E8F0),
            keySecondaryText = Color(0xFF7B889E),
            keyBorder = Color(0xFF232836),
            keyGlowBorder = Color(0xFF7DD3FC),
            enterButtonStart = Color(0xFF2563EB),
            enterButtonEnd = Color(0xFF7C3AED),
            suggestionChipFill = Color(0xFF151924),
        )
    }
}

class KeyboardSettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val SHOW_NUMBER_ROW = booleanPreferencesKey("show_number_row")
        val SHOW_KEY_POPUP = booleanPreferencesKey("show_key_popup")
        val NEON_GLOW_BORDERS = booleanPreferencesKey("neon_glow_borders")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val VIBRATION_STRENGTH_MS = intPreferencesKey("vibration_strength_ms")
        val AUTO_CAPITALIZE = booleanPreferencesKey("auto_capitalize")
        val PREDICTIVE_SUGGESTIONS = booleanPreferencesKey("predictive_suggestions")
        val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        val ROMAN_URDU_AUTO_CONVERT = booleanPreferencesKey("roman_urdu_auto_convert")
        val ACTIVE_LANGUAGE_CODE = stringPreferencesKey("active_language_code")
        val ENABLED_LANGUAGES = stringSetPreferencesKey("enabled_languages")
        val CUSTOM_TRANSLITERATIONS = stringSetPreferencesKey("custom_transliterations")
    }

    val settingsFlow: Flow<KeyboardSettings> = context.lkKeyboardDataStore.data.map { prefs ->
        val presetId = prefs[Keys.THEME_PRESET]
        val preset = KeyboardThemePreset.entries.firstOrNull { it.id == presetId }
            ?: KeyboardThemePreset.CYBER_CYAN
        val enabledSet = prefs[Keys.ENABLED_LANGUAGES]
            ?.takeIf { it.isNotEmpty() }
            ?: KeyboardLanguage.entries.map { it.code }.toSet()
        val activeLang = prefs[Keys.ACTIVE_LANGUAGE_CODE]
            ?.takeIf { enabledSet.contains(it) }
            ?: enabledSet.firstOrNull()
            ?: KeyboardLanguage.ENGLISH.code
        val customEntries = prefs[Keys.CUSTOM_TRANSLITERATIONS].orEmpty().mapNotNull { entry ->
            val idx = entry.indexOf("=>")
            if (idx > 0 && idx < entry.length - 2) {
                entry.substring(0, idx).lowercase() to entry.substring(idx + 2)
            } else {
                null
            }
        }.toMap()

        KeyboardSettings(
            themePreset = preset,
            showNumberRow = prefs[Keys.SHOW_NUMBER_ROW] ?: true,
            showKeyPopup = prefs[Keys.SHOW_KEY_POPUP] ?: true,
            neonGlowBorders = prefs[Keys.NEON_GLOW_BORDERS] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            vibrationStrengthMs = (prefs[Keys.VIBRATION_STRENGTH_MS] ?: 18).coerceIn(5, 50),
            autoCapitalize = prefs[Keys.AUTO_CAPITALIZE] ?: true,
            predictiveSuggestions = prefs[Keys.PREDICTIVE_SUGGESTIONS] ?: true,
            incognitoMode = prefs[Keys.INCOGNITO_MODE] ?: false,
            romanUrduAutoConvert = prefs[Keys.ROMAN_URDU_AUTO_CONVERT] ?: false,
            activeLanguageCode = activeLang,
            enabledLanguageCodes = enabledSet,
            customTransliterations = customEntries,
        )
    }

    suspend fun setThemePreset(preset: KeyboardThemePreset) {
        context.lkKeyboardDataStore.edit { it[Keys.THEME_PRESET] = preset.id }
    }

    suspend fun setShowNumberRow(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.SHOW_NUMBER_ROW] = enabled }
    }

    suspend fun setShowKeyPopup(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.SHOW_KEY_POPUP] = enabled }
    }

    suspend fun setNeonGlowBorders(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.NEON_GLOW_BORDERS] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setVibrationStrengthMs(ms: Int) {
        context.lkKeyboardDataStore.edit { it[Keys.VIBRATION_STRENGTH_MS] = ms.coerceIn(5, 50) }
    }

    suspend fun setAutoCapitalize(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.AUTO_CAPITALIZE] = enabled }
    }

    suspend fun setPredictiveSuggestions(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.PREDICTIVE_SUGGESTIONS] = enabled }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.INCOGNITO_MODE] = enabled }
    }

    suspend fun setRomanUrduAutoConvert(enabled: Boolean) {
        context.lkKeyboardDataStore.edit { it[Keys.ROMAN_URDU_AUTO_CONVERT] = enabled }
    }

    suspend fun setActiveLanguage(language: KeyboardLanguage) {
        context.lkKeyboardDataStore.edit { prefs ->
            val currentEnabled = prefs[Keys.ENABLED_LANGUAGES]?.toMutableSet()
                ?: KeyboardLanguage.entries.map { it.code }.toMutableSet()
            currentEnabled.add(language.code)
            prefs[Keys.ENABLED_LANGUAGES] = currentEnabled
            prefs[Keys.ACTIVE_LANGUAGE_CODE] = language.code
        }
    }

    suspend fun toggleLanguageEnabled(language: KeyboardLanguage, enabled: Boolean) {
        context.lkKeyboardDataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_LANGUAGES]?.toMutableSet()
                ?: KeyboardLanguage.entries.map { it.code }.toMutableSet()
            if (enabled) {
                current.add(language.code)
            } else if (current.size > 1) {
                current.remove(language.code)
                if (prefs[Keys.ACTIVE_LANGUAGE_CODE] == language.code) {
                    prefs[Keys.ACTIVE_LANGUAGE_CODE] = current.first()
                }
            }
            prefs[Keys.ENABLED_LANGUAGES] = current
        }
    }

    suspend fun addCustomTransliteration(roman: String, urdu: String) {
        val cleanRoman = roman.trim().lowercase()
        val cleanUrdu = urdu.trim()
        if (cleanRoman.isEmpty() || cleanUrdu.isEmpty()) return
        context.lkKeyboardDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_TRANSLITERATIONS]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$cleanRoman=>") }
            current.add("$cleanRoman=>$cleanUrdu")
            prefs[Keys.CUSTOM_TRANSLITERATIONS] = current
        }
    }

    suspend fun removeCustomTransliteration(roman: String) {
        val cleanRoman = roman.trim().lowercase()
        context.lkKeyboardDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_TRANSLITERATIONS]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$cleanRoman=>") }
            prefs[Keys.CUSTOM_TRANSLITERATIONS] = current
        }
    }
}
