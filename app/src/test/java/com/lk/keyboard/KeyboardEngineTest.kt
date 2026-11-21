package com.lk.keyboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.lk.keyboard.ui.theme.LKKeyboardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeyboardEngineTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `manifest registers KeyboardService as valid Android IME`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val serviceIntent = Intent("android.view.InputMethod").setPackage(context.packageName)
        val services = pm.queryIntentServices(serviceIntent, PackageManager.GET_META_DATA)
        assertTrue("KeyboardService should be discoverable via android.view.InputMethod", services.isNotEmpty())

        val imeServiceInfo = pm.getServiceInfo(
            ComponentName(context, KeyboardService::class.java),
            PackageManager.GET_META_DATA,
        )
        assertEquals("android.permission.BIND_INPUT_METHOD", imeServiceInfo.permission)
        assertTrue(imeServiceInfo.exported)
        assertTrue(imeServiceInfo.metaData.containsKey("android.view.im"))
    }

    @Test
    fun `typing letters shift backspace space and enter in normal EditText`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val editText = EditText(context)
        val editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        val ic = editText.onCreateInputConnection(editorInfo)

        val controller = KeyboardController()
        controller.applySettings(KeyboardSettings(autoCapitalize = false))
        controller.onStartInput(editorInfo, ic)

        // Tap Shift -> Single shift, type 'h' -> 'H', auto-resets to OFF, type 'i' -> 'i'
        controller.handleKeyPress(KeySpec("SHIFT", "", kind = KeyKind.SHIFT), ic, editorInfo)
        assertEquals(ShiftState.SINGLE, controller.uiState.value.shiftState)

        val hUpper = if (controller.uiState.value.shiftState != ShiftState.OFF) "H" else "h"
        controller.handleKeyPress(KeySpec(hUpper, hUpper), ic, editorInfo)
        assertEquals(ShiftState.OFF, controller.uiState.value.shiftState)

        val iLower = if (controller.uiState.value.shiftState != ShiftState.OFF) "I" else "i"
        controller.handleKeyPress(KeySpec(iLower, iLower), ic, editorInfo)
        assertEquals("Hi", editText.text.toString())

        // Space
        controller.handleKeyPress(KeySpec("English", " ", kind = KeyKind.SPACE), ic, editorInfo)
        assertEquals("Hi ", editText.text.toString())

        // Backspace deletes space
        controller.handleKeyPress(KeySpec("DEL", "", kind = KeyKind.BACKSPACE), ic, editorInfo)
        assertEquals("Hi", editText.text.toString())

        // Enter inserts newline in multiline field
        controller.handleKeyPress(KeySpec("ENTER", "", kind = KeyKind.ENTER), ic, editorInfo)
        assertEquals("Hi\n", editText.text.toString())

        // Long press Shift toggles Caps Lock
        controller.handleShiftLongPress()
        assertEquals(ShiftState.CAPS_LOCK, controller.uiState.value.shiftState)
        controller.handleKeyPress(KeySpec("O", "O"), ic, editorInfo)
        controller.handleKeyPress(KeySpec("K", "K"), ic, editorInfo)
        assertEquals(ShiftState.CAPS_LOCK, controller.uiState.value.shiftState)
        assertEquals("Hi\nOK", editText.text.toString())
    }

    @Test
    fun `password field hides suggestions and disables transliteration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val editorInfo = EditorInfo()
        val ic = editText.onCreateInputConnection(editorInfo)

        val controller = KeyboardController()
        controller.applySettings(KeyboardSettings(predictiveSuggestions = true, romanUrduAutoConvert = true))
        controller.onStartInput(editorInfo, ic)

        assertTrue(controller.uiState.value.isPasswordField)
        controller.handleKeyPress(KeySpec("s", "s"), ic, editorInfo)
        controller.handleKeyPress(KeySpec("a", "a"), ic, editorInfo)
        controller.handleKeyPress(KeySpec("l", "l"), ic, editorInfo)
        controller.handleKeyPress(KeySpec("a", "a"), ic, editorInfo)
        controller.handleKeyPress(KeySpec("m", "m"), ic, editorInfo)

        assertTrue("Suggestions must remain empty in password fields", controller.uiState.value.suggestions.isEmpty())
        assertEquals("", controller.uiState.value.currentWord)

        // Pressing space must NOT transliterate password text
        controller.handleKeyPress(KeySpec("English", " ", kind = KeyKind.SPACE), ic, editorInfo)
        assertEquals("salam ", editText.text.toString())
    }

    @Test
    fun `language cycling and extensible Roman Urdu transliteration work end to end`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val editText = EditText(context)
        val editorInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        val ic = editText.onCreateInputConnection(editorInfo)

        val languageManager = LanguageManager(mapOf("yaar" to "یار"))
        val controller = KeyboardController(languageManager)
        controller.applySettings(KeyboardSettings(autoCapitalize = false))
        controller.onStartInput(editorInfo, ic)

        assertEquals(KeyboardLanguage.ENGLISH, controller.uiState.value.language)
        controller.handleKeyPress(KeySpec("EN", "", kind = KeyKind.LANGUAGE_SWITCH), ic, editorInfo)
        assertEquals(KeyboardLanguage.URDU, controller.uiState.value.language)
        controller.handleKeyPress(KeySpec("UR", "", kind = KeyKind.LANGUAGE_SWITCH), ic, editorInfo)
        assertEquals(KeyboardLanguage.PASHTO, controller.uiState.value.language)
        controller.handleKeyPress(KeySpec("PS", "", kind = KeyKind.LANGUAGE_SWITCH), ic, editorInfo)
        assertEquals(KeyboardLanguage.ROMAN_URDU, controller.uiState.value.language)

        // Turn on Roman Urdu conversion toggle near space
        controller.handleKeyPress(KeySpec("RU→اردو", "", kind = KeyKind.ROMAN_URDU_TOGGLE), ic, editorInfo)
        assertTrue(controller.uiState.value.romanUrduConvertToggle)

        "yaar".forEach { ch ->
            controller.handleKeyPress(KeySpec(ch.toString(), ch.toString()), ic, editorInfo)
        }
        assertTrue(controller.uiState.value.suggestions.any { it.text == "یار" && it.isTransliteration })

        controller.handleKeyPress(KeySpec("Roman Urdu", " ", kind = KeyKind.SPACE), ic, editorInfo)
        assertEquals("یار ", editText.text.toString())
    }

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `keyboard view renders all core controls in portrait`() {
        val settings = KeyboardSettings()
        val palette = ThemeManager.paletteFor(settings.themePreset)
        var settingsClicked = false

        composeRule.setContent {
            LKKeyboardTheme {
                KeyboardView(
                    uiState = KeyboardUiState(
                        suggestions = listOf(SuggestionCandidate("Hello")),
                    ),
                    settings = settings,
                    palette = palette,
                    onKeyPress = {},
                    onShiftLongPress = {},
                    onSuggestionClick = {},
                    onInsertText = {},
                    onToggleOverlay = {},
                    onCloseOverlay = {},
                    onSelectLanguage = {},
                    onApplyAiTransform = {},
                    onVoiceClick = {},
                    onOpenSettings = { settingsClicked = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("lk_keyboard_root").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_tools_button").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_mic_button").assertIsDisplayed()
        composeRule.onNodeWithTag("keyboard_settings_button").assertIsDisplayed()
        composeRule.onNodeWithTag("key_shift").assertIsDisplayed()
        composeRule.onNodeWithTag("key_backspace").assertIsDisplayed()
        composeRule.onNodeWithTag("key_space").assertIsDisplayed()
        composeRule.onNodeWithTag("key_enter").assertIsDisplayed()
        composeRule.onNodeWithTag("key_roman_urdu_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("key_emoji").assertIsDisplayed()
        composeRule.onNodeWithTag("key_language").assertIsDisplayed()

        composeRule.onNodeWithTag("keyboard_settings_button").performClick()
        assertTrue(settingsClicked)
    }

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `keyboard view adapts cleanly in landscape and hides suggestions in password mode`() {
        val settings = KeyboardSettings()
        val palette = ThemeManager.paletteFor(settings.themePreset)

        composeRule.setContent {
            LKKeyboardTheme {
                KeyboardView(
                    uiState = KeyboardUiState(isPasswordField = true),
                    settings = settings,
                    palette = palette,
                    onKeyPress = {},
                    onShiftLongPress = {},
                    onSuggestionClick = {},
                    onInsertText = {},
                    onToggleOverlay = {},
                    onCloseOverlay = {},
                    onSelectLanguage = {},
                    onApplyAiTransform = {},
                    onVoiceClick = {},
                    onOpenSettings = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("lk_keyboard_root").assertIsDisplayed()
        composeRule.onNodeWithTag("password_privacy_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("key_space").assertIsDisplayed()
        composeRule.onNodeWithTag("key_enter").assertIsDisplayed()

        val imeService = Robolectric.buildService(KeyboardService::class.java).create().get()
        assertFalse("Fullscreen mode should remain disabled in landscape", imeService.onEvaluateFullscreenMode())
    }
}
