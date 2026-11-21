package com.lk.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lk.keyboard.ui.theme.LKKeyboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class KeyboardOverlayPanel {
    NONE,
    AI_TOOLS,
    EMOJI_PICKER,
    LANGUAGE_PICKER,
}

data class KeyboardUiState(
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val page: KeyboardPage = KeyboardPage.LETTERS,
    val shiftState: ShiftState = ShiftState.OFF,
    val isPasswordField: Boolean = false,
    val isIncognitoActive: Boolean = false,
    val enterAction: Int = EditorInfo.IME_ACTION_NONE,
    val isMultiLine: Boolean = true,
    val currentWord: String = "",
    val suggestions: List<SuggestionCandidate> = emptyList(),
    val romanUrduConvertToggle: Boolean = false,
    val overlayPanel: KeyboardOverlayPanel = KeyboardOverlayPanel.NONE,
    val isVoiceListening: Boolean = false,
    val voiceStatusMessage: String? = null,
)

class KeyboardController(
    private val languageManager: LanguageManager = LanguageManager(),
) {
    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private var currentSettings = KeyboardSettings()
    private var lastShiftTapTimestamp = 0L

    fun applySettings(settings: KeyboardSettings) {
        currentSettings = settings
        languageManager.updateCustomDictionary(settings.customTransliterations)
        val enabledLang = if (settings.enabledLanguageCodes.contains(_uiState.value.language.code)) {
            _uiState.value.language
        } else {
            KeyboardLanguage.fromCode(settings.activeLanguageCode)
        }
        _uiState.update { state ->
            state.copy(
                language = enabledLang,
                romanUrduConvertToggle = settings.romanUrduAutoConvert || state.romanUrduConvertToggle,
                isIncognitoActive = settings.incognitoMode || state.isIncognitoActive,
            )
        }
        refreshSuggestions()
    }

    fun onStartInput(editorInfo: EditorInfo?, inputConnection: InputConnection?) {
        val inputType = editorInfo?.inputType ?: 0
        val imeOptions = editorInfo?.imeOptions ?: 0
        val passwordField = isPasswordInputType(inputType)
        val noPersonalizedLearning = (imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        val numericField = isNumericOnlyInputType(inputType)
        val multiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0 ||
            (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        val action = imeOptions and EditorInfo.IME_MASK_ACTION

        val initialShift = if (
            !passwordField &&
            !numericField &&
            currentSettings.autoCapitalize &&
            (_uiState.value.language == KeyboardLanguage.ENGLISH ||
                _uiState.value.language == KeyboardLanguage.ROMAN_URDU)
        ) {
            val capsMode = inputConnection?.getCursorCapsMode(inputType) ?: 0
            if (capsMode != 0 || isCursorAtSentenceStart(inputConnection)) {
                ShiftState.SINGLE
            } else {
                ShiftState.OFF
            }
        } else {
            ShiftState.OFF
        }

        _uiState.update { state ->
            state.copy(
                page = if (numericField) KeyboardPage.NUMERIC_ONLY else KeyboardPage.LETTERS,
                shiftState = initialShift,
                isPasswordField = passwordField,
                isIncognitoActive = currentSettings.incognitoMode || noPersonalizedLearning,
                enterAction = action,
                isMultiLine = multiLine,
                currentWord = "",
                overlayPanel = KeyboardOverlayPanel.NONE,
                voiceStatusMessage = null,
            )
        }
        refreshSuggestions()
    }

    fun handleKeyPress(
        key: KeySpec,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
    ) {
        when (key.kind) {
            KeyKind.CHARACTER, KeyKind.PUNCTUATION -> {
                if (key.kind == KeyKind.PUNCTUATION) {
                    commitPunctuation(key.outputText, inputConnection, editorInfo)
                } else {
                    commitCharacter(key.outputText, inputConnection)
                }
            }

            KeyKind.SHIFT -> handleShiftTap()
            KeyKind.BACKSPACE -> handleBackspace(inputConnection, editorInfo)
            KeyKind.SPACE -> handleSpace(inputConnection, editorInfo)
            KeyKind.ENTER -> handleEnter(inputConnection)
            KeyKind.SYMBOLS_TOGGLE -> toggleSymbolsPage()
            KeyKind.LANGUAGE_SWITCH -> cycleLanguage(inputConnection, editorInfo)
            KeyKind.EMOJI_TOGGLE -> toggleOverlay(KeyboardOverlayPanel.EMOJI_PICKER)
            KeyKind.ROMAN_URDU_TOGGLE -> toggleRomanUrduConversion()
        }
    }

    fun handleShiftLongPress() {
        _uiState.update { state ->
            if (state.page == KeyboardPage.LETTERS) {
                val next = if (state.shiftState == ShiftState.CAPS_LOCK) ShiftState.OFF else ShiftState.CAPS_LOCK
                state.copy(shiftState = next)
            } else {
                state
            }
        }
    }

    fun selectLanguage(
        language: KeyboardLanguage,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
    ) {
        _uiState.update { state ->
            state.copy(
                language = language,
                page = KeyboardPage.LETTERS,
                overlayPanel = KeyboardOverlayPanel.NONE,
                shiftState = if (
                    !state.isPasswordField &&
                    currentSettings.autoCapitalize &&
                    (language == KeyboardLanguage.ENGLISH || language == KeyboardLanguage.ROMAN_URDU) &&
                    isCursorAtSentenceStart(inputConnection)
                ) {
                    ShiftState.SINGLE
                } else {
                    ShiftState.OFF
                },
            )
        }
        syncCurrentWordFromCursor(inputConnection, editorInfo)
        refreshSuggestions()
    }

    fun applySuggestion(candidate: SuggestionCandidate, inputConnection: InputConnection?) {
        val ic = inputConnection ?: return
        val state = _uiState.value
        if (state.isPasswordField) return

        val replaceLen = state.currentWord.length
        ic.beginBatchEdit()
        if (replaceLen > 0) {
            ic.deleteSurroundingText(replaceLen, 0)
        }
        ic.commitText("${candidate.text} ", 1)
        ic.endBatchEdit()

        _uiState.update { it.copy(currentWord = "") }
        refreshSuggestions()
    }

    fun insertDirectText(text: String, inputConnection: InputConnection?) {
        val ic = inputConnection ?: return
        ic.commitText(text, 1)
        if (!_uiState.value.isPasswordField) {
            _uiState.update { it.copy(currentWord = "") }
            refreshSuggestions()
        }
    }

    fun toggleOverlay(panel: KeyboardOverlayPanel) {
        _uiState.update { state ->
            val next = if (state.overlayPanel == panel) KeyboardOverlayPanel.NONE else panel
            state.copy(overlayPanel = next)
        }
    }

    fun closeOverlay() {
        _uiState.update { it.copy(overlayPanel = KeyboardOverlayPanel.NONE) }
    }

    fun toggleRomanUrduConversion() {
        _uiState.update { state ->
            state.copy(romanUrduConvertToggle = !state.romanUrduConvertToggle)
        }
        refreshSuggestions()
    }

    fun setVoiceState(isListening: Boolean, statusMessage: String? = null) {
        _uiState.update {
            it.copy(
                isVoiceListening = isListening,
                voiceStatusMessage = statusMessage,
            )
        }
    }

    fun applyLocalAiTransform(
        transform: LocalAiTransform,
        inputConnection: InputConnection?,
    ) {
        val ic = inputConnection ?: return
        val state = _uiState.value
        if (state.isPasswordField) return

        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrEmpty()) {
            val transformed = transformText(selected, transform)
            ic.commitText(transformed, 1)
            return
        }

        val word = state.currentWord.ifEmpty { extractLastWordBeforeCursor(ic) }
        if (word.isNotEmpty()) {
            val transformed = transformText(word, transform)
            ic.beginBatchEdit()
            ic.deleteSurroundingText(word.length, 0)
            ic.commitText(transformed, 1)
            ic.endBatchEdit()
            _uiState.update { it.copy(currentWord = transformed) }
            refreshSuggestions()
        }
    }

    private fun transformText(text: String, transform: LocalAiTransform): String = when (transform) {
        LocalAiTransform.ROMAN_TO_URDU -> languageManager.transliterateOrFallback(text)
        LocalAiTransform.UPPERCASE -> text.uppercase()
        LocalAiTransform.LOWERCASE -> text.lowercase()
        LocalAiTransform.TITLE_CASE -> text.split(" ").joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
        }
    }

    private fun commitCharacter(charText: String, inputConnection: InputConnection?) {
        inputConnection?.commitText(charText, 1)
        val state = _uiState.value
        val nextShift = if (state.page == KeyboardPage.LETTERS && state.shiftState == ShiftState.SINGLE) {
            ShiftState.OFF
        } else {
            state.shiftState
        }

        val updatedWord = if (state.isPasswordField) {
            ""
        } else if (charText.length == 1 && (charText[0].isLetterOrDigit() || charText[0] == '\'')) {
            state.currentWord + charText
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                shiftState = nextShift,
                currentWord = updatedWord,
            )
        }
        refreshSuggestions()
    }

    private fun commitPunctuation(
        punctuation: String,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
    ) {
        inputConnection?.commitText(punctuation, 1)
        val state = _uiState.value
        val shouldAutoCap = !state.isPasswordField &&
            currentSettings.autoCapitalize &&
            (punctuation == "." || punctuation == "۔" || punctuation == "?" || punctuation == "!") &&
            (state.language == KeyboardLanguage.ENGLISH || state.language == KeyboardLanguage.ROMAN_URDU)
        val nextShift = if (shouldAutoCap && state.shiftState == ShiftState.OFF) {
            ShiftState.SINGLE
        } else {
            state.shiftState
        }
        _uiState.update {
            it.copy(
                currentWord = "",
                shiftState = nextShift,
            )
        }
        if (editorInfo != null && state.isPasswordField) {
            _uiState.update { it.copy(currentWord = "", suggestions = emptyList()) }
        } else {
            refreshSuggestions()
        }
    }

    private fun handleShiftTap() {
        val state = _uiState.value
        when (state.page) {
            KeyboardPage.SYMBOLS_PRIMARY -> {
                _uiState.update { it.copy(page = KeyboardPage.SYMBOLS_SECONDARY) }
            }

            KeyboardPage.SYMBOLS_SECONDARY -> {
                _uiState.update { it.copy(page = KeyboardPage.SYMBOLS_PRIMARY) }
            }

            KeyboardPage.LETTERS -> {
                val now = System.currentTimeMillis()
                val isDoubleTap = (now - lastShiftTapTimestamp) in 1..420L
                lastShiftTapTimestamp = now
                val nextShift = when (state.shiftState) {
                    ShiftState.OFF -> ShiftState.SINGLE
                    ShiftState.SINGLE -> if (isDoubleTap) ShiftState.CAPS_LOCK else ShiftState.OFF
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
                _uiState.update { it.copy(shiftState = nextShift) }
            }

            KeyboardPage.NUMERIC_ONLY -> Unit
        }
    }

    private fun handleBackspace(inputConnection: InputConnection?, editorInfo: EditorInfo?) {
        val ic = inputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }

        val state = _uiState.value
        if (state.isPasswordField) {
            _uiState.update { it.copy(currentWord = "", suggestions = emptyList()) }
            return
        }

        val updatedWord = if (state.currentWord.isNotEmpty()) {
            state.currentWord.dropLast(1)
        } else {
            extractLastWordBeforeCursor(ic)
        }
        val nextShift = if (
            currentSettings.autoCapitalize &&
            state.page == KeyboardPage.LETTERS &&
            state.shiftState == ShiftState.OFF &&
            (state.language == KeyboardLanguage.ENGLISH || state.language == KeyboardLanguage.ROMAN_URDU) &&
            isCursorAtSentenceStart(ic)
        ) {
            ShiftState.SINGLE
        } else {
            state.shiftState
        }

        _uiState.update {
            it.copy(
                currentWord = updatedWord,
                shiftState = nextShift,
            )
        }
        if (editorInfo != null && isPasswordInputType(editorInfo.inputType)) {
            _uiState.update { it.copy(currentWord = "", suggestions = emptyList()) }
        } else {
            refreshSuggestions()
        }
    }

    private fun handleSpace(inputConnection: InputConnection?, editorInfo: EditorInfo?) {
        val ic = inputConnection ?: return
        val state = _uiState.value

        val shouldConvertRomanUrdu = !state.isPasswordField &&
            (state.romanUrduConvertToggle ||
                (state.language == KeyboardLanguage.ROMAN_URDU && currentSettings.romanUrduAutoConvert)) &&
            state.currentWord.isNotEmpty()

        if (shouldConvertRomanUrdu) {
            val translated = languageManager.transliterateWord(state.currentWord)
            if (translated != null) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(state.currentWord.length, 0)
                ic.commitText("$translated ", 1)
                ic.endBatchEdit()
            } else {
                ic.commitText(" ", 1)
            }
        } else {
            ic.commitText(" ", 1)
        }

        val shouldAutoCap = !state.isPasswordField &&
            currentSettings.autoCapitalize &&
            (state.language == KeyboardLanguage.ENGLISH || state.language == KeyboardLanguage.ROMAN_URDU) &&
            isCursorAtSentenceStart(ic)

        val nextShift = when {
            state.shiftState == ShiftState.CAPS_LOCK -> ShiftState.CAPS_LOCK
            shouldAutoCap -> ShiftState.SINGLE
            else -> ShiftState.OFF
        }

        _uiState.update {
            it.copy(
                currentWord = "",
                shiftState = nextShift,
            )
        }
        if (editorInfo != null && isPasswordInputType(editorInfo.inputType)) {
            _uiState.update { it.copy(suggestions = emptyList()) }
        } else {
            refreshSuggestions()
        }
    }

    private fun handleEnter(inputConnection: InputConnection?) {
        val ic = inputConnection ?: return
        val state = _uiState.value
        val hasAction = state.enterAction != EditorInfo.IME_ACTION_NONE &&
            state.enterAction != EditorInfo.IME_ACTION_UNSPECIFIED

        if (!state.isMultiLine && hasAction) {
            val handled = ic.performEditorAction(state.enterAction)
            if (!handled) {
                ic.commitText("\n", 1)
            }
        } else {
            ic.commitText("\n", 1)
        }

        val nextShift = if (
            !state.isPasswordField &&
            currentSettings.autoCapitalize &&
            state.shiftState != ShiftState.CAPS_LOCK &&
            (state.language == KeyboardLanguage.ENGLISH || state.language == KeyboardLanguage.ROMAN_URDU)
        ) {
            ShiftState.SINGLE
        } else {
            state.shiftState
        }

        _uiState.update {
            it.copy(
                currentWord = "",
                shiftState = nextShift,
            )
        }
        refreshSuggestions()
    }

    private fun toggleSymbolsPage() {
        _uiState.update { state ->
            val nextPage = when (state.page) {
                KeyboardPage.LETTERS -> KeyboardPage.SYMBOLS_PRIMARY
                KeyboardPage.SYMBOLS_PRIMARY, KeyboardPage.SYMBOLS_SECONDARY -> KeyboardPage.LETTERS
                KeyboardPage.NUMERIC_ONLY -> KeyboardPage.SYMBOLS_PRIMARY
            }
            state.copy(page = nextPage, overlayPanel = KeyboardOverlayPanel.NONE)
        }
    }

    private fun cycleLanguage(inputConnection: InputConnection?, editorInfo: EditorInfo?) {
        val next = languageManager.nextLanguage(
            current = _uiState.value.language,
            enabledCodes = currentSettings.enabledLanguageCodes,
        )
        selectLanguage(next, inputConnection, editorInfo)
    }

    private fun syncCurrentWordFromCursor(
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
    ) {
        if (inputConnection == null || isPasswordInputType(editorInfo?.inputType ?: 0)) {
            _uiState.update { it.copy(currentWord = "") }
            return
        }
        val extracted = extractLastWordBeforeCursor(inputConnection)
        _uiState.update { it.copy(currentWord = extracted) }
    }

    private fun extractLastWordBeforeCursor(inputConnection: InputConnection?): String {
        val before = inputConnection?.getTextBeforeCursor(40, 0)?.toString() ?: return ""
        if (before.isEmpty() || before.last().isWhitespace()) return ""
        return before.takeLastWhile { it.isLetterOrDigit() || it == '\'' }
    }

    private fun isCursorAtSentenceStart(inputConnection: InputConnection?): Boolean {
        val before = inputConnection?.getTextBeforeCursor(16, 0)?.toString() ?: return true
        val trimmed = before.trimEnd()
        if (trimmed.isEmpty()) return true
        val lastChar = trimmed.last()
        return before.endsWith("\n") ||
            (before.endsWith(" ") && (lastChar == '.' || lastChar == '!' || lastChar == '?' || lastChar == '۔'))
    }

    private fun refreshSuggestions() {
        val state = _uiState.value
        val showPredictions = currentSettings.predictiveSuggestions && !state.isIncognitoActive
        val suggestions = languageManager.getSuggestions(
            currentWord = state.currentWord,
            language = state.language,
            romanUrduConvertEnabled = state.romanUrduConvertToggle,
            isPasswordField = state.isPasswordField,
            predictionsEnabled = showPredictions,
        )
        _uiState.update { it.copy(suggestions = suggestions) }
    }

    companion object {
        fun isPasswordInputType(inputType: Int): Boolean {
            val klass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            return when (klass) {
                InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

                InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
                else -> false
            }
        }

        fun isNumericOnlyInputType(inputType: Int): Boolean {
            val klass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            if (klass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                return true
            }
            return klass == InputType.TYPE_CLASS_NUMBER || klass == InputType.TYPE_CLASS_PHONE
        }
    }
}

enum class LocalAiTransform {
    ROMAN_TO_URDU,
    TITLE_CASE,
    UPPERCASE,
    LOWERCASE,
}

class KeyboardService :
    InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var settingsRepository: KeyboardSettingsRepository
    private val languageManager = LanguageManager()
    val controller = KeyboardController(languageManager)

    private val settingsState = MutableStateFlow(KeyboardSettings())
    private var speechRecognizer: SpeechRecognizer? = null
    private var inputRootView: View? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        settingsRepository = KeyboardSettingsRepository(applicationContext)
        serviceScope.launch {
            settingsRepository.settingsFlow.collect { latest ->
                settingsState.value = latest
                controller.applySettings(latest)
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // Keep the target text field visible in both portrait and landscape modes.
        return false
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KeyboardService)
            setViewTreeViewModelStoreOwner(this@KeyboardService)
            setViewTreeSavedStateRegistryOwner(this@KeyboardService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val settings by settingsState.collectAsState()
                val uiState by controller.uiState.collectAsState()
                val palette = ThemeManager.paletteFor(settings.themePreset)

                LKKeyboardTheme {
                    KeyboardView(
                        uiState = uiState,
                        settings = settings,
                        palette = palette,
                        onKeyPress = { key ->
                            emitFeedback(settings)
                            controller.handleKeyPress(key, currentInputConnection, currentInputEditorInfo)
                        },
                        onShiftLongPress = {
                            emitFeedback(settings)
                            controller.handleShiftLongPress()
                        },
                        onSuggestionClick = { suggestion ->
                            emitFeedback(settings)
                            controller.applySuggestion(suggestion, currentInputConnection)
                        },
                        onInsertText = { text ->
                            emitFeedback(settings)
                            controller.insertDirectText(text, currentInputConnection)
                        },
                        onToggleOverlay = { panel ->
                            emitFeedback(settings)
                            controller.toggleOverlay(panel)
                        },
                        onCloseOverlay = {
                            controller.closeOverlay()
                        },
                        onSelectLanguage = { lang ->
                            emitFeedback(settings)
                            controller.selectLanguage(lang, currentInputConnection, currentInputEditorInfo)
                            serviceScope.launch {
                                settingsRepository.setActiveLanguage(lang)
                            }
                        },
                        onApplyAiTransform = { transform ->
                            emitFeedback(settings)
                            controller.applyLocalAiTransform(transform, currentInputConnection)
                        },
                        onVoiceClick = {
                            emitFeedback(settings)
                            handleVoiceTypingTrigger()
                        },
                        onOpenSettings = {
                            emitFeedback(settings)
                            openSettingsActivity(requestMicPermission = false)
                        },
                    )
                }
            }
        }
        inputRootView = composeView
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        controller.onStartInput(info, currentInputConnection)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        stopVoiceRecognition()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
    }

    override fun onDestroy() {
        stopVoiceRecognition()
        serviceScope.cancel()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
        super.onDestroy()
    }

    private fun handleVoiceTypingTrigger() {
        if (controller.uiState.value.isVoiceListening) {
            stopVoiceRecognition()
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            controller.setVoiceState(
                isListening = false,
                statusMessage = "Grant microphone permission in Settings to enable voice typing",
            )
            openSettingsActivity(requestMicPermission = true)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            controller.setVoiceState(
                isListening = false,
                statusMessage = "Speech recognition service unavailable on this device",
            )
            return
        }

        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer = recognizer

            val langTag = when (controller.uiState.value.language) {
                KeyboardLanguage.ENGLISH -> "en-US"
                KeyboardLanguage.URDU -> "ur-PK"
                KeyboardLanguage.PASHTO -> "ps-AF"
                KeyboardLanguage.ROMAN_URDU -> "en-PK"
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    controller.setVoiceState(true, "Listening ($langTag)… Tap mic to stop")
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    controller.setVoiceState(false, "Processing speech…")
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network unavailable for online speech"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        else -> "Voice input cancelled"
                    }
                    controller.setVoiceState(false, message)
                }

                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                    if (!spoken.isNullOrEmpty() && !controller.uiState.value.isPasswordField) {
                        currentInputConnection?.commitText("$spoken ", 1)
                    }
                    controller.setVoiceState(false, null)
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            controller.setVoiceState(true, "Initializing microphone…")
            recognizer.startListening(intent)
        } catch (_: SecurityException) {
            controller.setVoiceState(false, "Microphone permission required")
        } catch (_: Exception) {
            controller.setVoiceState(false, "Unable to start voice typing")
        }
    }

    private fun stopVoiceRecognition() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
            controller.setVoiceState(false, null)
        }
    }

    private fun openSettingsActivity(requestMicPermission: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (requestMicPermission) {
                putExtra(MainActivity.EXTRA_REQUEST_MIC_PERMISSION, true)
            }
        }
        startActivity(intent)
    }

    private fun emitFeedback(settings: KeyboardSettings) {
        if (settings.soundEnabled) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, -1f)
        }
        if (settings.vibrationEnabled) {
            val durationMs = settings.vibrationStrengthMs.toLong().coerceIn(5L, 50L)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            } else {
                inputRootView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }
}
