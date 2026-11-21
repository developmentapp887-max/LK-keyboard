# LK Keyboard (`com.lk.keyboard`)

A native Android Input Method Editor (IME) built with **Kotlin** and **Jetpack Compose** in a Dark AI visual style.

## Features
- **Real Android IME (`InputMethodService`)**: Registered with `BIND_INPUT_METHOD`, `android.view.InputMethod`, and multi-language subtypes (`res/xml/method.xml`).
- **Multilingual Layouts**: English (QWERTY), Urdu (اردو), Pashto (پښتو), and Roman Urdu, plus numeric keypad and dual symbol layers (`?123` / `=/<`).
- **Extensible Roman Urdu Transliteration**: Built-in Roman Urdu → Urdu script dictionary, spacebar toggle (`RU→ا`), phonetic fallback, and custom user dictionary entries.
- **Dark AI Design**: 4 selectable themes (`Cyber Cyan`, `Nebula Violet`, `Cobalt Matrix`, `Midnight OLED`), circular AI Tools button (local text transforms & quick replies), word suggestion bar, Emoji picker, Settings shortcut, and circular Microphone button.
- **Privacy First**:
  - Suggestions and local smart tools are automatically disabled in password fields and `IME_FLAG_NO_PERSONALIZED_LEARNING` / Incognito sessions.
  - Zero logging or cloud/Firebase upload of typed text or passwords.
  - `RECORD_AUDIO` permission is requested only when voice typing is activated, with clear disclosure of Android `SpeechRecognizer` handling.

## Project Structure
- `app/src/main/java/com/lk/keyboard/MainActivity.kt` — Companion settings & onboarding host
- `app/src/main/java/com/lk/keyboard/KeyboardService.kt` — Native `InputMethodService` & `KeyboardController`
- `app/src/main/java/com/lk/keyboard/KeyboardView.kt` — Responsive Jetpack Compose keyboard UI
- `app/src/main/java/com/lk/keyboard/KeyboardLayout.kt` — English, Urdu, Pashto, Roman Urdu, Symbols & Numeric layouts
- `app/src/main/java/com/lk/keyboard/LanguageManager.kt` — Multilingual suggestions & extensible Roman Urdu transliteration engine
- `app/src/main/java/com/lk/keyboard/ThemeManager.kt` — Dark AI color palettes & DataStore settings repository
- `app/src/main/java/com/lk/keyboard/SettingsScreen.kt` — Setup guide, live typing test pad, themes, feedback, language & privacy settings

## Building the Debug APK
Using Gradle (`9.3.1`) or the included GitHub Actions workflow (`.github/workflows/build-debug-apk.yml`):

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`
