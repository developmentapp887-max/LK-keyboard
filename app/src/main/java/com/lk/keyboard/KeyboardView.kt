package com.lk.keyboard

import android.view.inputmethod.EditorInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.KeyboardCapslock
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KeyboardView(
    uiState: KeyboardUiState,
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    onKeyPress: (KeySpec) -> Unit,
    onShiftLongPress: () -> Unit,
    onSuggestionClick: (SuggestionCandidate) -> Unit,
    onInsertText: (String) -> Unit,
    onToggleOverlay: (KeyboardOverlayPanel) -> Unit,
    onCloseOverlay: () -> Unit,
    onSelectLanguage: (KeyboardLanguage) -> Unit,
    onApplyAiTransform: (LocalAiTransform) -> Unit,
    onVoiceClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.backgroundTop, palette.backgroundBottom)
                )
            )
            .navigationBarsPadding()
            .testTag("lk_keyboard_root"),
    ) {
        val isLandscape = maxWidth > maxHeight && maxWidth >= 480.dp
        val isTabletWidth = maxWidth >= 680.dp
        val keyHeight = when {
            isLandscape -> 36.dp
            settings.showNumberRow -> 43.dp
            else -> 48.dp
        }
        val rowSpacing = if (isLandscape) 4.dp else 6.dp
        val keyHorizontalGap = if (isLandscape) 4.dp else 3.5.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isTabletWidth) {
                        Modifier
                            .widthIn(max = 820.dp)
                            .align(Alignment.Center)
                    } else {
                        Modifier
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Glowing neon top accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                palette.primaryAccent.copy(alpha = 0.85f),
                                palette.secondaryAccent.copy(alpha = 0.85f),
                                palette.tertiaryAccent.copy(alpha = 0.75f),
                            )
                        )
                    )
            )

            // Top AI Tools / Suggestions / Settings / Mic Strip
            KeyboardTopToolbar(
                uiState = uiState,
                settings = settings,
                palette = palette,
                onToggleAiTools = { onToggleOverlay(KeyboardOverlayPanel.AI_TOOLS) },
                onSuggestionClick = onSuggestionClick,
                onOpenSettings = onOpenSettings,
                onVoiceClick = onVoiceClick,
            )

            // Voice Listening Status Banner if active
            AnimatedVisibility(
                visible = uiState.isVoiceListening || !uiState.voiceStatusMessage.isNullOrEmpty(),
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
            ) {
                VoiceListeningBanner(
                    isListening = uiState.isVoiceListening,
                    message = uiState.voiceStatusMessage ?: "Listening…",
                    palette = palette,
                    onTapStop = onVoiceClick,
                )
            }

            // Main Keyboard Body or Active Overlay Panel
            val bodyModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 10.dp else 5.dp, vertical = 6.dp)

            when (uiState.overlayPanel) {
                KeyboardOverlayPanel.AI_TOOLS -> {
                    AiToolsPanel(
                        isPasswordField = uiState.isPasswordField,
                        language = uiState.language,
                        palette = palette,
                        panelHeight = (keyHeight * if (settings.showNumberRow) 5 else 4) + (rowSpacing * 4),
                        onApplyTransform = onApplyAiTransform,
                        onInsertSnippet = onInsertText,
                        onClose = onCloseOverlay,
                        modifier = bodyModifier,
                    )
                }

                KeyboardOverlayPanel.EMOJI_PICKER -> {
                    EmojiPickerPanel(
                        palette = palette,
                        panelHeight = (keyHeight * if (settings.showNumberRow) 5 else 4) + (rowSpacing * 4),
                        onEmojiClick = onInsertText,
                        onBackspace = {
                            onKeyPress(KeySpec("DEL", "", kind = KeyKind.BACKSPACE))
                        },
                        onClose = onCloseOverlay,
                        modifier = bodyModifier,
                    )
                }

                KeyboardOverlayPanel.LANGUAGE_PICKER -> {
                    LanguageQuickPickerPanel(
                        currentLanguage = uiState.language,
                        enabledCodes = settings.enabledLanguageCodes,
                        palette = palette,
                        panelHeight = (keyHeight * if (settings.showNumberRow) 5 else 4) + (rowSpacing * 4),
                        onSelectLanguage = onSelectLanguage,
                        onClose = onCloseOverlay,
                        modifier = bodyModifier,
                    )
                }

                KeyboardOverlayPanel.NONE -> {
                    val rows = remember(
                        uiState.page,
                        uiState.language,
                        uiState.shiftState,
                        settings.showNumberRow,
                    ) {
                        when (uiState.page) {
                            KeyboardPage.LETTERS -> KeyboardLayout.getLetterRows(
                                language = uiState.language,
                                shiftState = uiState.shiftState,
                                showNumberRow = settings.showNumberRow,
                            )

                            KeyboardPage.SYMBOLS_PRIMARY -> KeyboardLayout.getSymbolRows(
                                language = uiState.language,
                                secondaryPage = false,
                                showNumberRow = settings.showNumberRow,
                            )

                            KeyboardPage.SYMBOLS_SECONDARY -> KeyboardLayout.getSymbolRows(
                                language = uiState.language,
                                secondaryPage = true,
                                showNumberRow = settings.showNumberRow,
                            )

                            KeyboardPage.NUMERIC_ONLY -> KeyboardLayout.getNumericRows()
                        }
                    }

                    Column(
                        modifier = bodyModifier,
                        verticalArrangement = Arrangement.spacedBy(rowSpacing),
                    ) {
                        rows.forEachIndexed { rowIndex, rowKeys ->
                            val isMiddleNineKeyRow = uiState.page == KeyboardPage.LETTERS &&
                                ((settings.showNumberRow && rowIndex == 2) ||
                                    (!settings.showNumberRow && rowIndex == 1)) &&
                                rowKeys.size == 9

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isMiddleNineKeyRow) 14.dp else 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(keyHorizontalGap),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                rowKeys.forEach { keySpec ->
                                    KeyButton(
                                        key = keySpec,
                                        uiState = uiState,
                                        settings = settings,
                                        palette = palette,
                                        keyHeight = keyHeight,
                                        modifier = Modifier.weight(keySpec.weight),
                                        onPress = { onKeyPress(keySpec) },
                                        onLongPress = {
                                            when (keySpec.kind) {
                                                KeyKind.SHIFT -> onShiftLongPress()
                                                KeyKind.LANGUAGE_SWITCH -> onToggleOverlay(
                                                    KeyboardOverlayPanel.LANGUAGE_PICKER
                                                )

                                                else -> Unit
                                            }
                                        },
                                    )
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
private fun KeyboardTopToolbar(
    uiState: KeyboardUiState,
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    onToggleAiTools: () -> Unit,
    onSuggestionClick: (SuggestionCandidate) -> Unit,
    onOpenSettings: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.isVoiceListening) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "micPulseScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Circular AI Tools Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleAiTools)
                .testTag("ai_tools_button"),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.overlayPanel == KeyboardOverlayPanel.AI_TOOLS) {
                            Brush.linearGradient(listOf(palette.primaryAccent, palette.secondaryAccent))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    palette.primaryAccent.copy(alpha = 0.22f),
                                    palette.secondaryAccent.copy(alpha = 0.22f),
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(palette.primaryAccent, palette.secondaryAccent)
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "AI Tools",
                    tint = if (uiState.overlayPanel == KeyboardOverlayPanel.AI_TOOLS) {
                        palette.backgroundBottom
                    } else {
                        palette.primaryAccent
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Center: Suggestions OR Password Privacy Notice
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isPasswordField -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(palette.keyFill.copy(alpha = 0.7f))
                            .border(
                                width = 1.dp,
                                color = palette.primaryAccent.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(17.dp),
                            )
                            .padding(horizontal = 10.dp)
                            .testTag("password_privacy_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "Protected password mode",
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Password mode • Suggestions hidden",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.keySecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                !settings.predictiveSuggestions || uiState.isIncognitoActive -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(palette.keyFill.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Incognito active",
                            tint = palette.secondaryAccent,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isIncognitoActive) {
                                "Incognito • Local privacy active"
                            } else {
                                "Predictions paused"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.keySecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suggestion_bar"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val list = uiState.suggestions
                        if (list.isEmpty()) {
                            Text(
                                text = "${uiState.language.displayName} • Ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.keySecondaryText.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            list.forEachIndexed { idx, candidate ->
                                Surface(
                                    onClick = { onSuggestionClick(candidate) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (candidate.isTransliteration) {
                                        palette.primaryAccent.copy(alpha = 0.14f)
                                    } else {
                                        palette.suggestionChipFill
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (candidate.isTransliteration) {
                                            palette.primaryAccent.copy(alpha = 0.6f)
                                        } else {
                                            palette.keyBorder
                                        },
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("suggestion_chip_$idx"),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                    ) {
                                        Text(
                                            text = candidate.text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (candidate.isTransliteration) {
                                                    FontWeight.SemiBold
                                                } else {
                                                    FontWeight.Medium
                                                }
                                            ),
                                            color = if (candidate.isTransliteration) {
                                                palette.primaryAccent
                                            } else {
                                                palette.keyText
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Shortcut
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(48.dp)
                .testTag("keyboard_settings_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Keyboard Settings",
                tint = palette.keySecondaryText,
                modifier = Modifier.size(20.dp),
            )
        }

        // Right: Circular Microphone Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onVoiceClick)
                .testTag("voice_mic_button"),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(micPulseScale)
                    .clip(CircleShape)
                    .background(
                        if (uiState.isVoiceListening) {
                            palette.secondaryAccent
                        } else {
                            palette.secondaryAccent.copy(alpha = 0.18f)
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        color = if (uiState.isVoiceListening) palette.primaryAccent else palette.secondaryAccent,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Voice Typing",
                    tint = if (uiState.isVoiceListening) Color.White else palette.secondaryAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceListeningBanner(
    isListening: Boolean,
    message: String,
    palette: KeyboardPalette,
    onTapStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.secondaryAccent.copy(alpha = 0.16f))
            .clickable(onClick = onTapStop)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            tint = if (isListening) palette.primaryAccent else palette.secondaryAccent,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = palette.keyText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KeyButton(
    key: KeySpec,
    uiState: KeyboardUiState,
    settings: KeyboardSettings,
    palette: KeyboardPalette,
    keyHeight: Dp,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "keyScale",
    )

    val isSpecialActive = when (key.kind) {
        KeyKind.SHIFT -> uiState.page == KeyboardPage.LETTERS && uiState.shiftState != ShiftState.OFF
        KeyKind.ROMAN_URDU_TOGGLE -> uiState.romanUrduConvertToggle
        KeyKind.EMOJI_TOGGLE -> uiState.overlayPanel == KeyboardOverlayPanel.EMOJI_PICKER
        else -> false
    }

    val baseFill = when (key.kind) {
        KeyKind.CHARACTER, KeyKind.PUNCTUATION -> palette.keyFill
        KeyKind.SPACE -> palette.spaceKeyFill
        KeyKind.ENTER -> Color.Transparent
        KeyKind.SHIFT, KeyKind.ROMAN_URDU_TOGGLE ->
            if (isSpecialActive) palette.primaryAccent.copy(alpha = 0.22f) else palette.functionKeyFill
        else -> palette.functionKeyFill
    }

    val containerColor by animateColorAsState(
        targetValue = if (isPressed && key.kind != KeyKind.ENTER) palette.keyPressedFill else baseFill,
        animationSpec = tween(durationMillis = 80),
        label = "keyFill",
    )

    val borderColor = when {
        isPressed || isSpecialActive -> palette.keyGlowBorder
        settings.neonGlowBorders && key.kind == KeyKind.SPACE -> palette.primaryAccent.copy(alpha = 0.45f)
        settings.neonGlowBorders && key.kind == KeyKind.CHARACTER -> palette.keyBorder
        else -> palette.keyBorder.copy(alpha = 0.65f)
    }

    val keyTag = when (key.kind) {
        KeyKind.SHIFT -> "key_shift"
        KeyKind.BACKSPACE -> "key_backspace"
        KeyKind.SPACE -> "key_space"
        KeyKind.ENTER -> "key_enter"
        KeyKind.SYMBOLS_TOGGLE -> "key_symbols"
        KeyKind.LANGUAGE_SWITCH -> "key_language"
        KeyKind.EMOJI_TOGGLE -> "key_emoji"
        KeyKind.ROMAN_URDU_TOGGLE -> "key_roman_urdu_toggle"
        else -> "key_${key.primaryLabel}"
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .zIndex(if (isPressed) 10f else 0f)
            .testTag(keyTag),
        contentAlignment = Alignment.Center,
    ) {
        // Floating Key Preview Popup for character keys
        if (
            isPressed &&
            settings.showKeyPopup &&
            key.kind == KeyKind.CHARACTER &&
            key.primaryLabel.isNotEmpty()
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, -110) }
                    .size(width = 44.dp, height = 50.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.keyPressedFill)
                    .border(1.5.dp, palette.primaryAccent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = key.primaryLabel,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryAccent,
                )
            }
        }

        val shape = RoundedCornerShape(if (key.kind == KeyKind.SPACE) 12.dp else 10.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(shape)
                .then(
                    if (key.kind == KeyKind.ENTER) {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(palette.enterButtonStart, palette.enterButtonEnd)
                            )
                        )
                    } else {
                        Modifier
                            .background(containerColor)
                            .border(
                                width = if (isPressed || isSpecialActive) 1.3.dp else 0.9.dp,
                                color = borderColor,
                                shape = shape,
                            )
                    }
                )
                .pointerInput(key.kind, key.outputText) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true

                        var repeatJob: Job? = null
                        var longPressTriggered = false

                        if (key.kind == KeyKind.BACKSPACE) {
                            onPress()
                            repeatJob = coroutineScope.launch {
                                delay(380)
                                while (true) {
                                    onPress()
                                    delay(58)
                                }
                            }
                        } else if (key.kind == KeyKind.SHIFT || key.kind == KeyKind.LANGUAGE_SWITCH) {
                            repeatJob = coroutineScope.launch {
                                delay(430)
                                longPressTriggered = true
                                onLongPress()
                            }
                        }

                        val up = waitForUpOrCancellation()
                        repeatJob?.cancel()
                        isPressed = false

                        if (up != null && key.kind != KeyKind.BACKSPACE && !longPressTriggered) {
                            onPress()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            KeyCapContent(
                key = key,
                uiState = uiState,
                palette = palette,
                isSpecialActive = isSpecialActive,
            )
        }
    }
}

@Composable
private fun KeyCapContent(
    key: KeySpec,
    uiState: KeyboardUiState,
    palette: KeyboardPalette,
    isSpecialActive: Boolean,
) {
    when (key.kind) {
        KeyKind.SHIFT -> {
            if (uiState.page == KeyboardPage.SYMBOLS_PRIMARY || uiState.page == KeyboardPage.SYMBOLS_SECONDARY) {
                Text(
                    text = key.primaryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.keySecondaryText,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                val icon = when (uiState.shiftState) {
                    ShiftState.CAPS_LOCK -> Icons.Rounded.KeyboardCapslock
                    else -> Icons.Rounded.VerticalAlignTop
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Shift",
                        tint = if (isSpecialActive) palette.primaryAccent else palette.keyText,
                        modifier = Modifier.size(20.dp),
                    )
                    if (uiState.shiftState == ShiftState.CAPS_LOCK) {
                        Box(
                            modifier = Modifier
                                .size(width = 12.dp, height = 2.dp)
                                .clip(CircleShape)
                                .background(palette.primaryAccent)
                        )
                    }
                }
            }
        }

        KeyKind.BACKSPACE -> {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Backspace",
                tint = palette.keyText,
                modifier = Modifier.size(20.dp),
            )
        }

        KeyKind.ENTER -> {
            val actionIcon = when (uiState.enterAction) {
                EditorInfo.IME_ACTION_SEARCH -> Icons.Rounded.Search
                EditorInfo.IME_ACTION_SEND -> Icons.AutoMirrored.Rounded.Send
                EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_GO -> Icons.AutoMirrored.Rounded.ArrowForward
                EditorInfo.IME_ACTION_DONE -> Icons.Rounded.Check
                else -> Icons.AutoMirrored.Rounded.KeyboardReturn
            }
            Icon(
                imageVector = actionIcon,
                contentDescription = "Enter",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        KeyKind.EMOJI_TOGGLE -> {
            Icon(
                imageVector = Icons.Rounded.EmojiEmotions,
                contentDescription = "Emoji",
                tint = if (isSpecialActive) palette.primaryAccent else palette.keySecondaryText,
                modifier = Modifier.size(19.dp),
            )
        }

        KeyKind.LANGUAGE_SWITCH -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = "Switch Language",
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = key.primaryLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.keyText,
                )
            }
        }

        KeyKind.ROMAN_URDU_TOGGLE -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSpecialActive) palette.primaryAccent else palette.keySecondaryText.copy(alpha = 0.4f)
                        )
                )
                Text(
                    text = "RU→ا",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    ),
                    color = if (isSpecialActive) palette.primaryAccent else palette.keySecondaryText,
                    maxLines = 1,
                )
            }
        }

        KeyKind.SPACE -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = uiState.language.nativeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.keySecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(palette.primaryAccent, palette.secondaryAccent)
                            )
                        )
                )
            }
        }

        KeyKind.SYMBOLS_TOGGLE -> {
            Text(
                text = key.primaryLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.keySecondaryText,
            )
        }

        KeyKind.CHARACTER, KeyKind.PUNCTUATION -> {
            Box(modifier = Modifier.fillMaxSize()) {
                if (key.secondaryHint != null) {
                    Text(
                        text = key.secondaryHint,
                        fontSize = 9.sp,
                        color = palette.keySecondaryText.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 2.dp),
                    )
                }
                Text(
                    text = key.primaryLabel,
                    fontSize = if (uiState.language.isRtl && key.kind == KeyKind.CHARACTER) 19.sp else 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.keyText,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun AiToolsPanel(
    isPasswordField: Boolean,
    language: KeyboardLanguage,
    palette: KeyboardPalette,
    panelHeight: Dp,
    onApplyTransform: (LocalAiTransform) -> Unit,
    onInsertSnippet: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(panelHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.toolbarSurface)
            .border(1.dp, palette.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(10.dp)
            .testTag("ai_tools_panel"),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Local Smart Tools • Zero Upload",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.keyText,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close AI Tools",
                    tint = palette.keySecondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (isPasswordField) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Smart text tools are locked in password fields for your privacy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.keySecondaryText,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Transform Current / Selected Word",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.keySecondaryText,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToolActionChip("Roman → اردو", Icons.Rounded.Translate, palette, highlight = true) {
                        onApplyTransform(LocalAiTransform.ROMAN_TO_URDU)
                    }
                    ToolActionChip("Title Case", null, palette) {
                        onApplyTransform(LocalAiTransform.TITLE_CASE)
                    }
                    ToolActionChip("UPPER", null, palette) {
                        onApplyTransform(LocalAiTransform.UPPERCASE)
                    }
                    ToolActionChip("lower", null, palette) {
                        onApplyTransform(LocalAiTransform.LOWERCASE)
                    }
                }

                Text(
                    text = "Quick Replies (${language.displayName})",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.keySecondaryText,
                )
                val snippets = when (language) {
                    KeyboardLanguage.URDU -> listOf("السلام علیکم ", "شکریہ ", "ان شاء اللہ ", "خیریت سے ہوں ")
                    KeyboardLanguage.PASHTO -> listOf("سلام ", "ستړي مه شئ ", "مننه ", "په خیر راغلئ ")
                    KeyboardLanguage.ROMAN_URDU -> listOf("Assalam-o-Alaikum ", "Shukriya ", "Ji bilkul ", "Allah Hafiz ")
                    KeyboardLanguage.ENGLISH -> listOf("On my way! ", "Sounds good, thanks! ", "Let's connect soon. ", "Assalam-o-Alaikum! ")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snippets.forEach { snippet ->
                        ToolActionChip(snippet.trim(), null, palette) {
                            onInsertSnippet(snippet)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    palette: KeyboardPalette,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) palette.primaryAccent.copy(alpha = 0.16f) else palette.keyFill,
        border = BorderStroke(
            1.dp,
            if (highlight) palette.primaryAccent else palette.keyBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlight) palette.primaryAccent else palette.keyText,
            )
        }
    }
}

@Composable
private fun EmojiPickerPanel(
    palette: KeyboardPalette,
    panelHeight: Dp,
    onEmojiClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emojis = remember {
        listOf(
            "😀", "😃", "😄", "😁", "😊", "😍", "🥰", "🤩",
            "😂", "🤣", "😉", "😎", "🤔", "🤗", "🙏", "👍",
            "👏", "🔥", "✨", "⚡", "💜", "💙", "🩵", "❤️",
            "🌙", "⭐", "🇵🇰", "🕌", "🤖", "💡", "🎯", "🚀",
            "🎉", "🤝", "☕", "📚", "💻", "🎧", "🌟", "✅",
        )
    }

    Column(
        modifier = modifier
            .height(panelHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.toolbarSurface)
            .border(1.dp, palette.keyBorder, RoundedCornerShape(14.dp))
            .padding(8.dp)
            .testTag("emoji_picker_panel"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                onClick = onClose,
                shape = RoundedCornerShape(8.dp),
                color = palette.functionKeyFill,
            ) {
                Text(
                    text = "ABC",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryAccent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Text(
                text = "Emojis",
                style = MaterialTheme.typography.labelLarge,
                color = palette.keySecondaryText,
            )
            IconButton(onClick = onBackspace, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Backspace",
                    tint = palette.keyText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 42.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun LanguageQuickPickerPanel(
    currentLanguage: KeyboardLanguage,
    enabledCodes: Set<String>,
    palette: KeyboardPalette,
    panelHeight: Dp,
    onSelectLanguage: (KeyboardLanguage) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val languages = KeyboardLanguage.entries.filter { enabledCodes.contains(it.code) }
        .ifEmpty { listOf(KeyboardLanguage.ENGLISH) }

    Column(
        modifier = modifier
            .height(panelHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.toolbarSurface)
            .border(1.dp, palette.primaryAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Switch Input Language",
                style = MaterialTheme.typography.labelLarge,
                color = palette.keyText,
            )
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Language Picker",
                    tint = palette.keySecondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            languages.forEach { lang ->
                val selected = lang == currentLanguage
                Surface(
                    onClick = { onSelectLanguage(lang) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) palette.primaryAccent.copy(alpha = 0.16f) else palette.keyFill,
                    border = BorderStroke(
                        1.dp,
                        if (selected) palette.primaryAccent else palette.keyBorder,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${lang.displayName} (${lang.nativeLabel})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (selected) palette.primaryAccent else palette.keyText,
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active",
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
