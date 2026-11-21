package com.lk.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lk.keyboard.ui.theme.CobaltBlue
import com.lk.keyboard.ui.theme.ElectricPurple
import com.lk.keyboard.ui.theme.NeonCyan
import com.lk.keyboard.ui.theme.ObsidianBlack
import com.lk.keyboard.ui.theme.SuccessEmerald
import com.lk.keyboard.ui.theme.TextPrimaryDark
import com.lk.keyboard.ui.theme.TextSecondaryDark
import com.lk.keyboard.ui.theme.WarningAmber
import kotlin.math.roundToInt

enum class SettingsTab(val label: String) {
    ALL("All"),
    SETUP("Setup"),
    THEMES("Themes"),
    LANGUAGES("Languages"),
    PRIVACY("Privacy"),
}

@Composable
fun SettingsScreen(
    settings: KeyboardSettings,
    isImeEnabled: Boolean,
    isImeDefault: Boolean,
    hasMicPermission: Boolean,
    showVoiceConsentDialog: Boolean,
    onDismissVoiceConsentDialog: () -> Unit,
    onConfirmVoicePermission: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onOpenInputMethodPicker: () -> Unit,
    onSelectTheme: (KeyboardThemePreset) -> Unit,
    onToggleNumberRow: (Boolean) -> Unit,
    onToggleKeyPopup: (Boolean) -> Unit,
    onToggleNeonBorders: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onChangeVibrationStrength: (Int) -> Unit,
    onToggleLanguageEnabled: (KeyboardLanguage, Boolean) -> Unit,
    onSelectDefaultLanguage: (KeyboardLanguage) -> Unit,
    onToggleAutoCapitalize: (Boolean) -> Unit,
    onToggleRomanUrduAutoConvert: (Boolean) -> Unit,
    onAddCustomTransliteration: (String, String) -> Unit,
    onRemoveCustomTransliteration: (String) -> Unit,
    onTogglePredictiveSuggestions: (Boolean) -> Unit,
    onToggleIncognitoMode: (Boolean) -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.ALL) }
    var testText by remember { mutableStateOf("") }
    var passwordTestText by remember { mutableStateOf("") }
    var newRomanInput by remember { mutableStateOf("") }
    var newUrduInput by remember { mutableStateOf("") }

    val activePalette = remember(settings.themePreset) {
        ThemeManager.paletteFor(settings.themePreset)
    }

    if (showVoiceConsentDialog) {
        AlertDialog(
            onDismissRequest = onDismissVoiceConsentDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = activePalette.primaryAccent,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voice Typing Permission")
                }
            },
            text = {
                Text(
                    text = "Microphone access is requested only when you activate voice typing. " +
                        "Audio is processed by your device's Android SpeechRecognizer service " +
                        "(which may use on-device or online speech recognition configured on your device). " +
                        "LK Keyboard never logs, stores, or uploads typed text or passwords.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmVoicePermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activePalette.primaryAccent,
                        contentColor = ObsidianBlack,
                    ),
                ) {
                    Text("Continue & Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissVoiceConsentDialog) {
                    Text("Not Now", color = TextSecondaryDark)
                }
            },
        )
    }

    Scaffold(
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0F1E),
                            ObsidianBlack,
                            Color(0xFF070A12),
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .testTag("settings_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Hero Header
                item {
                    HeroHeaderCard(
                        isImeEnabled = isImeEnabled,
                        isImeDefault = isImeDefault,
                        palette = activePalette,
                    )
                }

                // Category Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SettingsTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            FilterChip(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = activePalette.primaryAccent.copy(alpha = 0.18f),
                                    selectedLabelColor = activePalette.primaryAccent,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = TextSecondaryDark,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = activePalette.primaryAccent,
                                ),
                            )
                        }
                    }
                }

                // 1. Setup & Live Test Pad
                if (selectedTab == SettingsTab.ALL || selectedTab == SettingsTab.SETUP) {
                    item {
                        SettingsSectionCard(
                            title = "Keyboard Setup & Activation",
                            icon = Icons.Rounded.Keyboard,
                            accent = activePalette.primaryAccent,
                        ) {
                            StepStatusRow(
                                step = "1",
                                title = "Enable LK Keyboard",
                                description = "Open Android On-screen Keyboard settings and turn on LK Keyboard.",
                                isDone = isImeEnabled,
                                buttonText = if (isImeEnabled) "Manage in Settings" else "Enable Keyboard",
                                accent = activePalette.primaryAccent,
                                buttonTag = "enable_keyboard_button",
                                onClick = onOpenImeSettings,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )

                            StepStatusRow(
                                step = "2",
                                title = "Select as Default Keyboard",
                                description = "Open the system input method switcher and pick LK Keyboard.",
                                isDone = isImeDefault,
                                buttonText = if (isImeDefault) "Switch Input Method" else "Set as Default",
                                accent = activePalette.secondaryAccent,
                                buttonTag = "select_default_keyboard_button",
                                onClick = onOpenInputMethodPicker,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )

                            Text(
                                text = "Live Typing Test Pad",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = testText,
                                onValueChange = { testText = it },
                                label = { Text("Tap here to test English, اردو, پښتو, or Roman Urdu") },
                                placeholder = { Text("Type with Shift, Space, Backspace, Enter…") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_text_field"),
                                colors = darkFieldColors(activePalette.primaryAccent),
                                minLines = 2,
                                maxLines = 4,
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = passwordTestText,
                                onValueChange = { passwordTestText = it },
                                label = { Text("Password Field Test (verifies hidden suggestions)") },
                                placeholder = { Text("Suggestions & learning auto-pause here") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_password_field"),
                                colors = darkFieldColors(activePalette.secondaryAccent),
                            )
                        }
                    }
                }

                // 2. Theme Settings
                if (selectedTab == SettingsTab.ALL || selectedTab == SettingsTab.THEMES) {
                    item {
                        SettingsSectionCard(
                            title = "Dark AI Themes & Layout",
                            icon = Icons.Rounded.Palette,
                            accent = activePalette.secondaryAccent,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                KeyboardThemePreset.entries.forEach { preset ->
                                    val palette = ThemeManager.paletteFor(preset)
                                    val isSelected = settings.themePreset == preset
                                    Surface(
                                        onClick = { onSelectTheme(preset) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) {
                                            palette.primaryAccent.copy(alpha = 0.12f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                palette.primaryAccent
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("theme_preset_${preset.id}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // Color Swatch Preview
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                SwatchDot(palette.backgroundTop, palette.keyBorder)
                                                SwatchDot(palette.primaryAccent, palette.primaryAccent)
                                                SwatchDot(palette.secondaryAccent, palette.secondaryAccent)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = preset.displayName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimaryDark,
                                                )
                                                Text(
                                                    text = preset.subtitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextSecondaryDark,
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Selected theme",
                                                    tint = palette.primaryAccent,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            ToggleSettingRow(
                                title = "Dedicated Number Row",
                                subtitle = "Always show digits 1–0 above the top letter row",
                                checked = settings.showNumberRow,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_number_row",
                                onCheckedChange = onToggleNumberRow,
                            )
                            ToggleSettingRow(
                                title = "Key Press Preview Popup",
                                subtitle = "Show floating neon bubble when pressing letter keys",
                                checked = settings.showKeyPopup,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_key_popup",
                                onCheckedChange = onToggleKeyPopup,
                            )
                            ToggleSettingRow(
                                title = "Neon Glow Key Borders",
                                subtitle = "Subtle illuminated borders on Space and active toggles",
                                checked = settings.neonGlowBorders,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_neon_borders",
                                onCheckedChange = onToggleNeonBorders,
                            )
                        }
                    }

                    // 3. Sound & Vibration Settings
                    item {
                        SettingsSectionCard(
                            title = "Sound & Haptic Vibration",
                            icon = Icons.Rounded.Vibration,
                            accent = CobaltBlue,
                        ) {
                            ToggleSettingRow(
                                title = "Keypress Sound",
                                subtitle = "Play crisp system click sound on key taps",
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                checked = settings.soundEnabled,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_sound",
                                onCheckedChange = onToggleSound,
                            )
                            ToggleSettingRow(
                                title = "Keypress Vibration",
                                subtitle = "Tactile haptic pulse on every key press",
                                icon = Icons.Rounded.Vibration,
                                checked = settings.vibrationEnabled,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_vibration",
                                onCheckedChange = onToggleVibration,
                            )

                            AnimatedVisibility(visible = settings.vibrationEnabled) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "Vibration Duration",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondaryDark,
                                        )
                                        Text(
                                            text = "${settings.vibrationStrengthMs} ms",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = activePalette.primaryAccent,
                                        )
                                    }
                                    Slider(
                                        value = settings.vibrationStrengthMs.toFloat(),
                                        onValueChange = { onChangeVibrationStrength(it.roundToInt()) },
                                        valueRange = 5f..50f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = activePalette.primaryAccent,
                                            activeTrackColor = activePalette.primaryAccent,
                                            inactiveTrackColor = MaterialTheme.colorScheme.outline,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Language Selection & Transliteration Dictionary
                if (selectedTab == SettingsTab.ALL || selectedTab == SettingsTab.LANGUAGES) {
                    item {
                        SettingsSectionCard(
                            title = "Languages & Roman Urdu Engine",
                            icon = Icons.Rounded.Language,
                            accent = activePalette.primaryAccent,
                        ) {
                            Text(
                                text = "Enabled Layouts (tap chip to set default)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                KeyboardLanguage.entries.forEach { language ->
                                    val enabled = settings.enabledLanguageCodes.contains(language.code)
                                    val isDefault = settings.activeLanguageCode == language.code
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isDefault) {
                                                    activePalette.primaryAccent.copy(alpha = 0.1f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                }
                                            )
                                            .clickable(enabled = enabled) {
                                                onSelectDefaultLanguage(language)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = activePalette.primaryAccent.copy(alpha = 0.16f),
                                        ) {
                                            Text(
                                                text = language.shortBadge,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = activePalette.primaryAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${language.displayName} • ${language.nativeLabel}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimaryDark,
                                            )
                                            if (isDefault) {
                                                Text(
                                                    text = "Default active layout",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = activePalette.primaryAccent,
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = enabled,
                                            onCheckedChange = { onToggleLanguageEnabled(language, it) },
                                            colors = switchColors(activePalette.primaryAccent),
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )

                            ToggleSettingRow(
                                title = "Auto-Capitalize Sentences",
                                subtitle = "Automatically shift first letter after sentence punctuation",
                                checked = settings.autoCapitalize,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_auto_cap",
                                onCheckedChange = onToggleAutoCapitalize,
                            )
                            ToggleSettingRow(
                                title = "Roman Urdu Auto-Convert on Space",
                                subtitle = "Convert recognized Roman Urdu words to Urdu script when pressing Space",
                                icon = Icons.Rounded.Translate,
                                checked = settings.romanUrduAutoConvert,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_roman_urdu_auto",
                                onCheckedChange = onToggleRomanUrduAutoConvert,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )

                            Text(
                                text = "Extensible Transliteration Dictionary",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryDark,
                            )
                            Text(
                                text = "${LanguageManager.BUILT_IN_ROMAN_TO_URDU.size} built-in pairs + " +
                                    "${settings.customTransliterations.size} custom local entries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = newRomanInput,
                                    onValueChange = { newRomanInput = it },
                                    label = { Text("Roman") },
                                    placeholder = { Text("yaar") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = darkFieldColors(activePalette.primaryAccent),
                                )
                                OutlinedTextField(
                                    value = newUrduInput,
                                    onValueChange = { newUrduInput = it },
                                    label = { Text("Urdu") },
                                    placeholder = { Text("یار") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = darkFieldColors(activePalette.primaryAccent),
                                )
                                IconButton(
                                    onClick = {
                                        if (newRomanInput.isNotBlank() && newUrduInput.isNotBlank()) {
                                            onAddCustomTransliteration(newRomanInput, newUrduInput)
                                            newRomanInput = ""
                                            newUrduInput = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(activePalette.primaryAccent.copy(alpha = 0.18f)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add transliteration word",
                                        tint = activePalette.primaryAccent,
                                    )
                                }
                            }

                            if (settings.customTransliterations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                settings.customTransliterations.forEach { (roman, urdu) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "$roman  →  $urdu",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimaryDark,
                                        )
                                        IconButton(
                                            onClick = { onRemoveCustomTransliteration(roman) },
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Remove custom word",
                                                tint = TextSecondaryDark,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Privacy & Voice Settings
                if (selectedTab == SettingsTab.ALL || selectedTab == SettingsTab.PRIVACY) {
                    item {
                        SettingsSectionCard(
                            title = "Privacy & Voice Processing",
                            icon = Icons.Rounded.PrivacyTip,
                            accent = SuccessEmerald,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessEmerald.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shield,
                                            contentDescription = null,
                                            tint = SuccessEmerald,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Strict Local Privacy Guarantees",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SuccessEmerald,
                                        )
                                    }
                                    Text(
                                        text = "• Passwords are never collected, logged, cached, or transmitted.\n" +
                                            "• Predictive suggestions & AI tools automatically lock in password fields.\n" +
                                            "• Zero keyboard input is uploaded to Firebase or external servers.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimaryDark,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleSettingRow(
                                title = "Predictive Word Suggestions",
                                subtitle = "Show offline dictionary completions (always hidden in passwords)",
                                icon = Icons.Rounded.SettingsSuggest,
                                checked = settings.predictiveSuggestions,
                                accent = activePalette.primaryAccent,
                                tag = "toggle_predictions",
                                onCheckedChange = onTogglePredictiveSuggestions,
                            )
                            ToggleSettingRow(
                                title = "Incognito Keyboard Mode",
                                subtitle = "Pause suggestions and local smart context across all apps",
                                icon = Icons.Rounded.Lock,
                                checked = settings.incognitoMode,
                                accent = activePalette.secondaryAccent,
                                tag = "toggle_incognito",
                                onCheckedChange = onToggleIncognitoMode,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Voice Typing & Microphone",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryDark,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Microphone permission is requested only when you activate voice typing. " +
                                            "Speech recognition is handled by your device's Android SpeechRecognizer " +
                                            "(which may use online processing depending on your device speech provider).",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondaryDark,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                StatusPill(
                                    text = if (hasMicPermission) "Mic Permission Granted" else "Mic Permission Not Set",
                                    isPositive = hasMicPermission,
                                )
                                OutlinedButton(
                                    onClick = onRequestMicPermission,
                                    enabled = !hasMicPermission,
                                    border = BorderStroke(
                                        1.dp,
                                        if (hasMicPermission) SuccessEmerald.copy(alpha = 0.4f)
                                        else activePalette.primaryAccent,
                                    ),
                                    modifier = Modifier.testTag("grant_mic_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (hasMicPermission) "Enabled" else "Enable Voice Typing")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroHeaderCard(
    isImeEnabled: Boolean,
    isImeDefault: Boolean,
    palette: KeyboardPalette,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                listOf(palette.primaryAccent.copy(alpha = 0.7f), palette.secondaryAccent.copy(alpha = 0.7f))
            ),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            palette.primaryAccent.copy(alpha = 0.12f),
                            palette.secondaryAccent.copy(alpha = 0.08f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.primaryAccent, palette.secondaryAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "LK Keyboard logo",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LK Keyboard",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimaryDark,
                        )
                        Text(
                            text = "Dark AI Multilingual IME • EN / اردو / پښتو / Roman",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = if (isImeEnabled) "IME Enabled" else "Not Enabled Yet",
                    isPositive = isImeEnabled,
                )
                StatusPill(
                    text = if (isImeDefault) "Active Default" else "Not Default",
                    isPositive = isImeDefault,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    isPositive: Boolean,
) {
    val color = if (isPositive) SuccessEmerald else WarningAmber
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryDark,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StepStatusRow(
    step: String,
    title: String,
    description: String,
    isDone: Boolean,
    buttonText: String,
    accent: Color,
    buttonTag: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDone) SuccessEmerald.copy(alpha = 0.2f) else accent.copy(alpha = 0.2f))
                    .border(1.dp, if (isDone) SuccessEmerald else accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDone) SuccessEmerald else accent,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryDark,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                )
            }
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.copy(alpha = 0.18f),
                contentColor = accent,
            ),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(buttonTag),
        ) {
            Text(text = buttonText, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    tag: String,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryDark,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = switchColors(accent),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun SwatchDot(fill: Color, border: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, border, CircleShape)
    )
}

@Composable
private fun switchColors(accent: Color) = SwitchDefaults.colors(
    checkedThumbColor = ObsidianBlack,
    checkedTrackColor = accent,
    uncheckedThumbColor = TextSecondaryDark,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)

@Composable
private fun darkFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = accent,
    unfocusedLabelColor = TextSecondaryDark,
    cursorColor = NeonCyan,
    focusedTextColor = TextPrimaryDark,
    unfocusedTextColor = TextPrimaryDark,
)
