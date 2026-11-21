package com.lk.keyboard

enum class KeyboardPage {
    LETTERS,
    SYMBOLS_PRIMARY,
    SYMBOLS_SECONDARY,
    NUMERIC_ONLY,
}

enum class ShiftState {
    OFF,
    SINGLE,
    CAPS_LOCK,
}

enum class KeyKind {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    SYMBOLS_TOGGLE,
    LANGUAGE_SWITCH,
    EMOJI_TOGGLE,
    ROMAN_URDU_TOGGLE,
    PUNCTUATION,
}

data class KeySpec(
    val primaryLabel: String,
    val outputText: String = primaryLabel,
    val secondaryHint: String? = null,
    val kind: KeyKind = KeyKind.CHARACTER,
    val weight: Float = 1f,
)

object KeyboardLayout {

    val numberRow: List<KeySpec> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        .map { KeySpec(primaryLabel = it, outputText = it, kind = KeyKind.CHARACTER, weight = 1f) }

    fun getLetterRows(
        language: KeyboardLanguage,
        shiftState: ShiftState,
        showNumberRow: Boolean,
    ): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()
        if (showNumberRow) {
            rows.add(
                if (language == KeyboardLanguage.URDU || language == KeyboardLanguage.PASHTO) {
                    if (shiftState != ShiftState.OFF) numberRow else rtlDigitOrLatinRow(language)
                } else {
                    numberRow
                }
            )
        }

        when (language) {
            KeyboardLanguage.ENGLISH, KeyboardLanguage.ROMAN_URDU -> {
                val isUpper = shiftState != ShiftState.OFF
                val r1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
                val r2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
                val r3 = listOf("z", "x", "c", "v", "b", "n", "m")

                rows.add(r1.mapIndexed { idx, ch ->
                    val out = if (isUpper) ch.uppercase() else ch
                    val hint = ((idx + 1) % 10).toString()
                    KeySpec(primaryLabel = out, outputText = out, secondaryHint = hint)
                })
                rows.add(r2.map { ch ->
                    val out = if (isUpper) ch.uppercase() else ch
                    KeySpec(primaryLabel = out, outputText = out)
                })
                rows.add(
                    buildList {
                        add(KeySpec(primaryLabel = "SHIFT", outputText = "", kind = KeyKind.SHIFT, weight = 1.45f))
                        r3.forEach { ch ->
                            val out = if (isUpper) ch.uppercase() else ch
                            add(KeySpec(primaryLabel = out, outputText = out))
                        }
                        add(KeySpec(primaryLabel = "DEL", outputText = "", kind = KeyKind.BACKSPACE, weight = 1.45f))
                    }
                )
            }

            KeyboardLanguage.URDU -> {
                val shifted = shiftState != ShiftState.OFF
                val r1 = if (!shifted) {
                    listOf("ق", "و", "ع", "ر", "ت", "ے", "ء", "ی", "ہ", "پ")
                } else {
                    listOf("ْ", "ؤ", "ث", "ڑ", "ٹ", "ۓ", "ئ", "ئ", "ح", "ُ")
                }
                val r2 = if (!shifted) {
                    listOf("ا", "س", "د", "ف", "گ", "ح", "ج", "ک", "ل")
                } else {
                    listOf("آ", "ص", "ڈ", "ٰ", "غ", "ھ", "ض", "خ", "ً")
                }
                val r3 = if (!shifted) {
                    listOf("ز", "ش", "چ", "ط", "ب", "ن", "م")
                } else {
                    listOf("ذ", "ژ", "ظ", "ں", "پ", "ں", "ّ")
                }

                rows.add(r1.map { KeySpec(primaryLabel = it, outputText = it) })
                rows.add(r2.map { KeySpec(primaryLabel = it, outputText = it) })
                rows.add(
                    buildList {
                        add(KeySpec(primaryLabel = "SHIFT", outputText = "", kind = KeyKind.SHIFT, weight = 1.45f))
                        r3.forEach { add(KeySpec(primaryLabel = it, outputText = it)) }
                        add(KeySpec(primaryLabel = "DEL", outputText = "", kind = KeyKind.BACKSPACE, weight = 1.45f))
                    }
                )
            }

            KeyboardLanguage.PASHTO -> {
                val shifted = shiftState != ShiftState.OFF
                val r1 = if (!shifted) {
                    listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")
                } else {
                    listOf("څ", "ځ", "ښ", "ږ", "ټ", "ډ", "ړ", "ڼ", "ګ", "ې", "ۍ", "ئ")
                }
                val r2 = if (!shifted) {
                    listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ک", "ګ")
                } else {
                    listOf("ؤ", "ء", "ی", "پ", "ږ", "آ", "ټ", "ڼ", "ة", "غ", "ژ")
                }
                val r3 = if (!shifted) {
                    listOf("څ", "ځ", "ز", "ر", "ذ", "د", "و", "ښ", "ڼ")
                } else {
                    listOf("ډ", "ړ", "ژ", "ط", "ظ", "ذ", "و", "ې", "ۍ")
                }

                rows.add(r1.map { KeySpec(primaryLabel = it, outputText = it) })
                rows.add(r2.map { KeySpec(primaryLabel = it, outputText = it) })
                rows.add(
                    buildList {
                        add(KeySpec(primaryLabel = "SHIFT", outputText = "", kind = KeyKind.SHIFT, weight = 1.35f))
                        r3.forEach { add(KeySpec(primaryLabel = it, outputText = it)) }
                        add(KeySpec(primaryLabel = "DEL", outputText = "", kind = KeyKind.BACKSPACE, weight = 1.35f))
                    }
                )
            }
        }

        rows.add(buildBottomRow(language, isSymbols = false))
        return rows
    }

    fun getSymbolRows(
        language: KeyboardLanguage,
        secondaryPage: Boolean,
        showNumberRow: Boolean,
    ): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()
        if (showNumberRow || !secondaryPage) {
            rows.add(numberRow)
        } else {
            rows.add(
                listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "Δ")
                    .map { KeySpec(primaryLabel = it, outputText = it) }
            )
        }

        if (!secondaryPage) {
            rows.add(
                listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
                    .map { KeySpec(primaryLabel = it, outputText = it) }
            )
            rows.add(
                listOf("*", "\"", "'", ":", ";", "!", "?", "؟", "،")
                    .map { KeySpec(primaryLabel = it, outputText = it) }
            )
            rows.add(
                buildList {
                    add(KeySpec(primaryLabel = "=\\<", outputText = "", kind = KeyKind.SHIFT, weight = 1.45f))
                    listOf("%", "=", "[", "]", "{", "}", "\\").forEach {
                        add(KeySpec(primaryLabel = it, outputText = it))
                    }
                    add(KeySpec(primaryLabel = "DEL", outputText = "", kind = KeyKind.BACKSPACE, weight = 1.45f))
                }
            )
        } else {
            rows.add(
                listOf("£", "€", "¥", "¢", "^", "°", "=", "{", "}", "\\")
                    .map { KeySpec(primaryLabel = it, outputText = it) }
            )
            rows.add(
                listOf("%", "©", "®", "™", "✓", "[", "]", "<", ">")
                    .map { KeySpec(primaryLabel = it, outputText = it) }
            )
            rows.add(
                buildList {
                    add(KeySpec(primaryLabel = "?123", outputText = "", kind = KeyKind.SHIFT, weight = 1.45f))
                    listOf("₹", "₨", "§", "≈", "≠", "≤", "≥").forEach {
                        add(KeySpec(primaryLabel = it, outputText = it))
                    }
                    add(KeySpec(primaryLabel = "DEL", outputText = "", kind = KeyKind.BACKSPACE, weight = 1.45f))
                }
            )
        }

        rows.add(buildBottomRow(language, isSymbols = true))
        return rows
    }

    fun getNumericRows(): List<List<KeySpec>> = listOf(
        listOf("1", "2", "3", "-").map { KeySpec(it, it) },
        listOf("4", "5", "6", "+").map { KeySpec(it, it) },
        listOf("7", "8", "9", "*").map { KeySpec(it, it) },
        listOf(
            KeySpec(".", ".", kind = KeyKind.PUNCTUATION, weight = 1f),
            KeySpec("0", "0", weight = 1f),
            KeySpec("DEL", "", kind = KeyKind.BACKSPACE, weight = 1f),
            KeySpec("ENTER", "", kind = KeyKind.ENTER, weight = 1f),
        ),
    )

    private fun buildBottomRow(
        language: KeyboardLanguage,
        isSymbols: Boolean,
    ): List<KeySpec> {
        val commaChar = if (language == KeyboardLanguage.URDU || language == KeyboardLanguage.PASHTO) "،" else ","
        val periodChar = if (language == KeyboardLanguage.URDU || language == KeyboardLanguage.PASHTO) "۔" else "."
        return listOf(
            KeySpec(
                primaryLabel = if (isSymbols) "ABC" else "?123",
                outputText = "",
                kind = KeyKind.SYMBOLS_TOGGLE,
                weight = 1.25f,
            ),
            KeySpec(
                primaryLabel = "EMOJI",
                outputText = "",
                kind = KeyKind.EMOJI_TOGGLE,
                weight = 0.95f,
            ),
            KeySpec(
                primaryLabel = language.shortBadge,
                outputText = "",
                kind = KeyKind.LANGUAGE_SWITCH,
                weight = 1.0f,
            ),
            KeySpec(
                primaryLabel = commaChar,
                outputText = commaChar,
                kind = KeyKind.PUNCTUATION,
                weight = 0.75f,
            ),
            KeySpec(
                primaryLabel = language.nativeLabel,
                outputText = " ",
                kind = KeyKind.SPACE,
                weight = 3.1f,
            ),
            KeySpec(
                primaryLabel = "RU→اردو",
                outputText = "",
                kind = KeyKind.ROMAN_URDU_TOGGLE,
                weight = 1.25f,
            ),
            KeySpec(
                primaryLabel = periodChar,
                outputText = periodChar,
                kind = KeyKind.PUNCTUATION,
                weight = 0.75f,
            ),
            KeySpec(
                primaryLabel = "ENTER",
                outputText = "",
                kind = KeyKind.ENTER,
                weight = 1.45f,
            ),
        )
    }

    private fun rtlDigitOrLatinRow(language: KeyboardLanguage): List<KeySpec> =
        if (language == KeyboardLanguage.URDU || language == KeyboardLanguage.PASHTO) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                .map { KeySpec(primaryLabel = it, outputText = it) }
        } else {
            numberRow
        }
}
