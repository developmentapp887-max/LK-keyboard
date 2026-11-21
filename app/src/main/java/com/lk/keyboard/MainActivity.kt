package com.lk.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lk.keyboard.ui.theme.LKKeyboardTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REQUEST_MIC_PERMISSION = "extra_request_mic_permission"
    }

    private lateinit var settingsRepository: KeyboardSettingsRepository
    private val promptVoiceOnLaunch = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsRepository = KeyboardSettingsRepository(applicationContext)
        promptVoiceOnLaunch.value = intent?.getBooleanExtra(EXTRA_REQUEST_MIC_PERMISSION, false) == true

        setContent {
            LKKeyboardTheme {
                val settings by settingsRepository.settingsFlow.collectAsState(initial = KeyboardSettings())
                val coroutineScope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                var isImeEnabled by remember { mutableStateOf(checkImeEnabled()) }
                var isImeDefault by remember { mutableStateOf(checkImeDefault()) }
                var hasMicPermission by remember { mutableStateOf(checkMicPermission()) }
                var showVoiceConsent by remember(promptVoiceOnLaunch.value, hasMicPermission) {
                    mutableStateOf(promptVoiceOnLaunch.value && !hasMicPermission)
                }

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    hasMicPermission = granted
                    promptVoiceOnLaunch.value = false
                    showVoiceConsent = false
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isImeEnabled = checkImeEnabled()
                            isImeDefault = checkImeDefault()
                            hasMicPermission = checkMicPermission()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                SettingsScreen(
                    settings = settings,
                    isImeEnabled = isImeEnabled,
                    isImeDefault = isImeDefault,
                    hasMicPermission = hasMicPermission,
                    showVoiceConsentDialog = showVoiceConsent,
                    onDismissVoiceConsentDialog = {
                        promptVoiceOnLaunch.value = false
                        showVoiceConsent = false
                    },
                    onConfirmVoicePermission = {
                        showVoiceConsent = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onOpenImeSettings = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    },
                    onOpenInputMethodPicker = {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    onSelectTheme = { preset ->
                        coroutineScope.launch { settingsRepository.setThemePreset(preset) }
                    },
                    onToggleNumberRow = { enabled ->
                        coroutineScope.launch { settingsRepository.setShowNumberRow(enabled) }
                    },
                    onToggleKeyPopup = { enabled ->
                        coroutineScope.launch { settingsRepository.setShowKeyPopup(enabled) }
                    },
                    onToggleNeonBorders = { enabled ->
                        coroutineScope.launch { settingsRepository.setNeonGlowBorders(enabled) }
                    },
                    onToggleSound = { enabled ->
                        coroutineScope.launch { settingsRepository.setSoundEnabled(enabled) }
                    },
                    onToggleVibration = { enabled ->
                        coroutineScope.launch { settingsRepository.setVibrationEnabled(enabled) }
                    },
                    onChangeVibrationStrength = { ms ->
                        previewVibration(ms)
                        coroutineScope.launch { settingsRepository.setVibrationStrengthMs(ms) }
                    },
                    onToggleLanguageEnabled = { language, enabled ->
                        coroutineScope.launch {
                            settingsRepository.toggleLanguageEnabled(language, enabled)
                        }
                    },
                    onSelectDefaultLanguage = { language ->
                        coroutineScope.launch { settingsRepository.setActiveLanguage(language) }
                    },
                    onToggleAutoCapitalize = { enabled ->
                        coroutineScope.launch { settingsRepository.setAutoCapitalize(enabled) }
                    },
                    onToggleRomanUrduAutoConvert = { enabled ->
                        coroutineScope.launch { settingsRepository.setRomanUrduAutoConvert(enabled) }
                    },
                    onAddCustomTransliteration = { roman, urdu ->
                        coroutineScope.launch {
                            settingsRepository.addCustomTransliteration(roman, urdu)
                        }
                    },
                    onRemoveCustomTransliteration = { roman ->
                        coroutineScope.launch {
                            settingsRepository.removeCustomTransliteration(roman)
                        }
                    },
                    onTogglePredictiveSuggestions = { enabled ->
                        coroutineScope.launch { settingsRepository.setPredictiveSuggestions(enabled) }
                    },
                    onToggleIncognitoMode = { enabled ->
                        coroutineScope.launch { settingsRepository.setIncognitoMode(enabled) }
                    },
                    onRequestMicPermission = {
                        if (!checkMicPermission()) {
                            showVoiceConsent = true
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_REQUEST_MIC_PERMISSION, false)) {
            promptVoiceOnLaunch.value = true
        }
    }

    private fun checkImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun checkImeDefault(): Boolean {
        val currentDefault = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        ) ?: return false
        return currentDefault.startsWith("$packageName/")
    }

    private fun checkMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    private fun previewVibration(durationMs: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs.toLong().coerceIn(5L, 50L), VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
}
