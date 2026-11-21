package com.lk.keyboard

enum class KeyboardLanguage(
    val code: String,
    val displayName: String,
    val nativeLabel: String,
    val shortBadge: String,
    val isRtl: Boolean,
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeLabel = "English",
        shortBadge = "EN",
        isRtl = false,
    ),
    URDU(
        code = "ur",
        displayName = "Urdu",
        nativeLabel = "اردو",
        shortBadge = "UR",
        isRtl = true,
    ),
    PASHTO(
        code = "ps",
        displayName = "Pashto",
        nativeLabel = "پښتو",
        shortBadge = "PS",
        isRtl = true,
    ),
    ROMAN_URDU(
        code = "ru",
        displayName = "Roman Urdu",
        nativeLabel = "Roman Urdu",
        shortBadge = "RU",
        isRtl = false,
    );

    companion object {
        fun fromCode(code: String?): KeyboardLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

data class SuggestionCandidate(
    val text: String,
    val isTransliteration: Boolean = false,
    val sourceRoman: String? = null,
)

class LanguageManager(
    initialCustomDictionary: Map<String, String> = emptyMap(),
) {
    private val runtimeDictionary = mutableMapOf<String, String>().apply {
        putAll(initialCustomDictionary.mapKeys { it.key.lowercase() })
    }
    private val settingsDictionary = mutableMapOf<String, String>()

    fun updateCustomDictionary(entries: Map<String, String>) {
        settingsDictionary.clear()
        entries.forEach { (roman, urdu) ->
            val key = roman.trim().lowercase()
            val value = urdu.trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                settingsDictionary[key] = value
            }
        }
    }

    fun registerTransliteration(roman: String, urdu: String) {
        val key = roman.trim().lowercase()
        val value = urdu.trim()
        if (key.isNotEmpty() && value.isNotEmpty()) {
            runtimeDictionary[key] = value
        }
    }

    fun nextLanguage(
        current: KeyboardLanguage,
        enabledCodes: Set<String>,
    ): KeyboardLanguage {
        val enabled = KeyboardLanguage.entries.filter { enabledCodes.contains(it.code) }
            .ifEmpty { listOf(KeyboardLanguage.ENGLISH) }
        val index = enabled.indexOf(current)
        return if (index == -1) enabled.first() else enabled[(index + 1) % enabled.size]
    }

    fun transliterateWord(romanWord: String): String? {
        val normalized = romanWord.trim().lowercase()
        if (normalized.isEmpty()) return null
        settingsDictionary[normalized]?.let { return it }
        runtimeDictionary[normalized]?.let { return it }
        BUILT_IN_ROMAN_TO_URDU[normalized]?.let { return it }
        return null
    }

    fun transliterateOrFallback(romanWord: String): String {
        val clean = romanWord.trim()
        if (clean.isEmpty()) return ""
        transliterateWord(clean)?.let { return it }
        return phoneticFallbackRomanToUrdu(clean.lowercase())
    }

    fun getSuggestions(
        currentWord: String,
        language: KeyboardLanguage,
        romanUrduConvertEnabled: Boolean,
        isPasswordField: Boolean,
        predictionsEnabled: Boolean,
    ): List<SuggestionCandidate> {
        if (isPasswordField || !predictionsEnabled) return emptyList()

        val query = currentWord.trim()
        if (query.isEmpty()) {
            return when (language) {
                KeyboardLanguage.ENGLISH -> DEFAULT_ENGLISH_STARTERS.map { SuggestionCandidate(it) }
                KeyboardLanguage.URDU -> DEFAULT_URDU_STARTERS.map { SuggestionCandidate(it) }
                KeyboardLanguage.PASHTO -> DEFAULT_PASHTO_STARTERS.map { SuggestionCandidate(it) }
                KeyboardLanguage.ROMAN_URDU -> DEFAULT_ROMAN_STARTERS.map { word ->
                    val urdu = transliterateWord(word)
                    if (romanUrduConvertEnabled && urdu != null) {
                        SuggestionCandidate(urdu, isTransliteration = true, sourceRoman = word)
                    } else {
                        SuggestionCandidate(word)
                    }
                }
            }
        }

        val lower = query.lowercase()
        val results = mutableListOf<SuggestionCandidate>()

        val shouldOfferTransliteration =
            language == KeyboardLanguage.ROMAN_URDU ||
                (language == KeyboardLanguage.ENGLISH && romanUrduConvertEnabled)

        if (shouldOfferTransliteration) {
            val exactUrdu = transliterateWord(lower)
            if (exactUrdu != null) {
                results.add(
                    SuggestionCandidate(
                        text = exactUrdu,
                        isTransliteration = true,
                        sourceRoman = query,
                    )
                )
            } else if (romanUrduConvertEnabled && lower.length >= 2) {
                results.add(
                    SuggestionCandidate(
                        text = phoneticFallbackRomanToUrdu(lower),
                        isTransliteration = true,
                        sourceRoman = query,
                    )
                )
            }
        }

        when (language) {
            KeyboardLanguage.ENGLISH -> {
                val matches = ENGLISH_WORDS
                    .filter { it.startsWith(lower) && it != lower }
                    .take(3 - results.size)
                matches.forEach { word ->
                    val cased = if (query.firstOrNull()?.isUpperCase() == true) {
                        word.replaceFirstChar { it.uppercaseChar() }
                    } else {
                        word
                    }
                    results.add(SuggestionCandidate(cased))
                }
                if (romanUrduConvertEnabled && results.size < 3) {
                    mergedRomanDictionaryKeys()
                        .filter { it.startsWith(lower) && it != lower }
                        .take(3 - results.size)
                        .forEach { roman ->
                            val urdu = transliterateWord(roman) ?: roman
                            results.add(
                                SuggestionCandidate(
                                    text = urdu,
                                    isTransliteration = true,
                                    sourceRoman = roman,
                                )
                            )
                        }
                }
            }

            KeyboardLanguage.ROMAN_URDU -> {
                mergedRomanDictionaryKeys()
                    .filter { it.startsWith(lower) && it != lower }
                    .take(3)
                    .forEach { roman ->
                        if (results.size < 3) {
                            val urdu = transliterateWord(roman)
                            if (romanUrduConvertEnabled && urdu != null) {
                                results.add(
                                    SuggestionCandidate(
                                        text = urdu,
                                        isTransliteration = true,
                                        sourceRoman = roman,
                                    )
                                )
                            } else {
                                results.add(SuggestionCandidate(roman))
                            }
                        }
                    }
                if (results.none { it.text.equals(query, ignoreCase = true) } && results.size < 3) {
                    results.add(SuggestionCandidate(query))
                }
            }

            KeyboardLanguage.URDU -> {
                URDU_WORDS
                    .filter { it.startsWith(query) && it != query }
                    .take(3)
                    .forEach { results.add(SuggestionCandidate(it)) }
            }

            KeyboardLanguage.PASHTO -> {
                PASHTO_WORDS
                    .filter { it.startsWith(query) && it != query }
                    .take(3)
                    .forEach { results.add(SuggestionCandidate(it)) }
            }
        }

        return results.distinctBy { it.text }.take(3)
    }

    private fun mergedRomanDictionaryKeys(): List<String> =
        (settingsDictionary.keys + runtimeDictionary.keys + BUILT_IN_ROMAN_TO_URDU.keys).distinct()

    private fun phoneticFallbackRomanToUrdu(word: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < word.length) {
            val two = if (i + 1 < word.length) word.substring(i, i + 2) else ""
            val mappedTwo = when (two) {
                "kh" -> "خ"
                "gh" -> "غ"
                "sh" -> "ش"
                "ch" -> "چ"
                "th" -> "تھ"
                "ph" -> "ف"
                "bh" -> "بھ"
                "dh" -> "دھ"
                "jh" -> "جھ"
                "aa" -> "آ"
                "ee", "ii" -> "ی"
                "oo", "uu" -> "و"
                "ai", "ay" -> "ے"
                else -> null
            }
            if (mappedTwo != null) {
                sb.append(mappedTwo)
                i += 2
                continue
            }

            val ch = word[i]
            val mappedOne = when (ch) {
                'a' -> if (i == 0) "ا" else ""
                'b' -> "ب"
                'c' -> "ک"
                'd' -> "د"
                'e' -> if (i == word.lastIndex) "ے" else "ی"
                'f' -> "ف"
                'g' -> "گ"
                'h' -> "ہ"
                'i' -> if (i == 0) "ا" else "ی"
                'j' -> "ج"
                'k' -> "ک"
                'l' -> "ل"
                'm' -> "م"
                'n' -> "ن"
                'o' -> "و"
                'p' -> "پ"
                'q' -> "ق"
                'r' -> "ر"
                's' -> "س"
                't' -> "ت"
                'u' -> "و"
                'v', 'w' -> "و"
                'x' -> "کس"
                'y' -> "ی"
                'z' -> "ز"
                else -> ch.toString()
            }
            sb.append(mappedOne)
            i++
        }
        return sb.toString().ifEmpty { word }
    }

    companion object {
        private val DEFAULT_ENGLISH_STARTERS = listOf("I", "The", "Thanks")
        private val DEFAULT_URDU_STARTERS = listOf("اسلام علیکم", "شکریہ", "کیسے ہیں")
        private val DEFAULT_PASHTO_STARTERS = listOf("سلام", "مننه", "څنګه یې")
        private val DEFAULT_ROMAN_STARTERS = listOf("salam", "kya", "shukriya")

        val BUILT_IN_ROMAN_TO_URDU: Map<String, String> = linkedMapOf(
            // Greetings & Politeness
            "salam" to "سلام",
            "assalam" to "السلام",
            "assalamualaikum" to "السلام علیکم",
            "walaikum" to "وعلیکم",
            "shukriya" to "شکریہ",
            "mehrbani" to "مہربانی",
            "meherbani" to "مہربانی",
            "maaf" to "معاف",
            "khushamdeed" to "خوش آمدید",
            "mubarak" to "مبارک",
            "khuda" to "خدا",
            "hafiz" to "حافظ",
            "allah" to "اللہ",
            "inshallah" to "ان شاء اللہ",
            "mashallah" to "ماشاءاللہ",
            "alhamdulillah" to "الحمدللہ",
            "subhanallah" to "سبحان اللہ",
            "jazakallah" to "جزاک اللہ",
            "dua" to "دعا",
            "ameen" to "آمین",

            // Pronouns & Core Grammar
            "main" to "میں",
            "mein" to "میں",
            "hum" to "ہم",
            "tum" to "تم",
            "aap" to "آپ",
            "woh" to "وہ",
            "wo" to "وہ",
            "yeh" to "یہ",
            "ye" to "یہ",
            "mera" to "میرا",
            "meri" to "میری",
            "mere" to "میرے",
            "tera" to "تیرا",
            "tumhara" to "تمہارا",
            "aapka" to "آپ کا",
            "hamara" to "ہمارا",
            "un" to "ان",
            "is" to "اس",
            "us" to "اس",
            "hai" to "ہے",
            "hain" to "ہیں",
            "hoon" to "ہوں",
            "hun" to "ہوں",
            "tha" to "تھا",
            "thi" to "تھی",
            "the" to "تھے",
            "ho" to "ہو",
            "hoga" to "ہوگا",
            "hogi" to "ہوگی",
            "aur" to "اور",
            "ya" to "یا",
            "lekin" to "لیکن",
            "magar" to "مگر",
            "agar" to "اگر",
            "toh" to "تو",
            "bhi" to "بھی",
            "hi" to "ہی",
            "se" to "سے",
            "ko" to "کو",
            "ka" to "کا",
            "ki" to "کی",
            "ke" to "کے",
            "par" to "پر",
            "tak" to "تک",
            "saath" to "ساتھ",

            // Questions & Responses
            "kya" to "کیا",
            "kaise" to "کیسے",
            "kaisi" to "کیسی",
            "kahan" to "کہاں",
            "kab" to "کب",
            "kyun" to "کیوں",
            "kyunke" to "کیونکہ",
            "kaun" to "کون",
            "kitna" to "کتنا",
            "kitne" to "کتنے",
            "haan" to "ہاں",
            "han" to "ہاں",
            "ji" to "جی",
            "nahi" to "نہیں",
            "na" to "نہ",
            "bilkul" to "بالکل",
            "zaroor" to "ضرور",
            "shayad" to "شاید",
            "theek" to "ٹھیک",
            "acha" to "اچھا",
            "achha" to "اچھا",
            "bura" to "برا",
            "bohat" to "بہت",
            "bahut" to "بہت",
            "kam" to "کم",
            "zyada" to "زیادہ",
            "sab" to "سب",
            "kuch" to "کچھ",

            // Everyday Verbs & Phrases
            "haal" to "حال",
            "chal" to "چل",
            "chalo" to "چلو",
            "aao" to "آؤ",
            "aana" to "آنا",
            "jao" to "جاؤ",
            "jana" to "جانا",
            "karo" to "کرو",
            "karna" to "کرنا",
            "kiya" to "کیا",
            "raha" to "رہا",
            "rahi" to "رہی",
            "rahe" to "رہے",
            "dekho" to "دیکھو",
            "dekhna" to "دیکھنا",
            "suno" to "سنو",
            "sunna" to "سننا",
            "bolo" to "بولو",
            "bolna" to "بولنا",
            "likho" to "لکھو",
            "likhna" to "لکھنا",
            "parho" to "پڑھو",
            "parhna" to "پڑھنا",
            "samjho" to "سمجھو",
            "socho" to "سوچو",
            "batao" to "بتاؤ",
            "batana" to "بتانا",
            "milna" to "ملنا",
            "mile" to "ملے",
            "ruko" to "رکو",
            "madad" to "مدد",
            "intezar" to "انتظار",
            "faisla" to "فیصلہ",
            "koshish" to "کوشش",

            // Family, People & Places
            "dost" to "دوست",
            "bhai" to "بھائی",
            "behan" to "بہن",
            "ammi" to "امی",
            "abbu" to "ابو",
            "ghar" to "گھر",
            "shehar" to "شہر",
            "mulk" to "ملک",
            "watan" to "وطن",
            "pakistan" to "پاکستان",
            "karachi" to "کراچی",
            "lahore" to "لاہور",
            "islamabad" to "اسلام آباد",
            "peshawar" to "پشاور",
            "quetta" to "کوئٹہ",
            "multan" to "ملتان",
            "kashmir" to "کشمیر",

            // Time, Nature & Emotions
            "waqt" to "وقت",
            "din" to "دن",
            "raat" to "رات",
            "aaj" to "آج",
            "kal" to "کل",
            "abhi" to "ابھی",
            "phir" to "پھر",
            "hamesha" to "ہمیشہ",
            "subah" to "صبح",
            "shaam" to "شام",
            "zindagi" to "زندگی",
            "duniya" to "دنیا",
            "khush" to "خوش",
            "khushi" to "خوشی",
            "pyar" to "پیار",
            "mohabbat" to "محبت",
            "muhabbat" to "محبت",
            "ishq" to "عشق",
            "dil" to "دل",
            "jaan" to "جان",
            "yaad" to "یاد",
            "khwab" to "خواب",
            "umeed" to "امید",
            "roshni" to "روشنی",
            "andhera" to "اندھیرا",
            "chand" to "چاند",
            "suraj" to "سورج",
            "sitara" to "ستارہ",
            "barish" to "بارش",
            "hawa" to "ہوا",
            "aasman" to "آسمان",
            "zameen" to "زمین",
            "phool" to "پھول",
            "baat" to "بات",
            "kahani" to "کہانی",
            "lafz" to "لفظ",
            "awaaz" to "آواز",
            "nazar" to "نظر",
            "waada" to "وعدہ",
            "bharosa" to "بھروسہ",
            "ehsaas" to "احساس",
            "qismat" to "قسمت",
            "naseeb" to "نصیب",

            // Food & Daily Nouns
            "khana" to "کھانا",
            "pani" to "پانی",
            "chai" to "چائے",
            "doodh" to "دودھ",
            "roti" to "روٹی",
            "kaam" to "کام",
            "kitab" to "کتاب",
            "qalam" to "قلم",
            "ilm" to "علم",
            "sawal" to "سوال",
            "jawab" to "جواب",
            "safar" to "سفر",
            "azadi" to "آزادی",
            "eid" to "عید",
            "namaz" to "نماز",
            "roza" to "روزہ",
        )

        val ENGLISH_WORDS: List<String> = listOf(
            "about", "above", "access", "account", "across", "action", "active", "actually",
            "after", "again", "against", "agree", "ahead", "almost", "alone", "along",
            "already", "also", "always", "amazing", "among", "amount", "android", "another",
            "answer", "anyone", "anything", "anyway", "around", "arrive", "article", "ask",
            "assistant", "attention", "available", "away", "awesome", "back", "balance", "based",
            "because", "become", "before", "begin", "behind", "believe", "below", "best",
            "better", "between", "beyond", "board", "book", "both", "bring", "brother",
            "build", "building", "business", "busy", "call", "came", "camera", "cancel",
            "care", "carry", "case", "cause", "center", "certain", "change", "check",
            "choice", "choose", "city", "class", "clean", "clear", "close", "cloud",
            "code", "coffee", "cold", "color", "come", "coming", "comment", "common",
            "company", "complete", "computer", "confirm", "connect", "consider", "contact", "continue",
            "control", "cool", "copy", "correct", "could", "country", "course", "cover",
            "create", "current", "custom", "daily", "dark", "data", "date", "days",
            "deal", "decide", "deep", "default", "delete", "deliver", "design", "detail",
            "device", "different", "direct", "discover", "discuss", "display", "distance", "done",
            "door", "down", "download", "dream", "drive", "during", "each", "early",
            "earth", "easy", "edit", "effect", "either", "email", "enable", "energy",
            "english", "enjoy", "enough", "enter", "equal", "error", "especially", "even",
            "evening", "event", "ever", "every", "everyone", "everything", "exact", "example",
            "excited", "excuse", "expect", "experience", "explain", "explore", "extra", "face",
            "fact", "family", "fast", "father", "favorite", "feature", "feel", "feeling",
            "field", "file", "final", "finally", "find", "fine", "finish", "first",
            "focus", "follow", "food", "force", "forget", "form", "forward", "found",
            "free", "friend", "friends", "from", "front", "full", "future", "game",
            "general", "getting", "give", "glad", "glass", "global", "going", "good",
            "google", "great", "green", "group", "grow", "guess", "guide", "hand",
            "handle", "happen", "happy", "hard", "have", "head", "health", "hear",
            "heart", "hello", "help", "here", "high", "history", "hold", "home",
            "honest", "hope", "hour", "house", "however", "human", "idea", "ideal",
            "image", "important", "improve", "include", "info", "information", "input", "inside",
            "instead", "interest", "internet", "into", "issue", "item", "join", "journey",
            "just", "keep", "keyboard", "kind", "king", "knew", "know", "knowledge",
            "land", "language", "large", "last", "late", "later", "latest", "launch",
            "learn", "least", "leave", "left", "less", "letter", "level", "life",
            "light", "like", "line", "link", "list", "listen", "little", "live",
            "local", "lock", "long", "look", "looking", "love", "lucky", "lunch",
            "made", "magic", "mail", "main", "make", "making", "manage", "many",
            "market", "match", "matter", "maybe", "mean", "meaning", "meet", "meeting",
            "member", "memory", "message", "method", "might", "mind", "minute", "miss",
            "mobile", "mode", "model", "modern", "moment", "money", "month", "more",
            "morning", "most", "mother", "motion", "move", "movie", "much", "music",
            "must", "name", "natural", "nature", "near", "nearly", "need", "network",
            "never", "news", "next", "nice", "night", "none", "normal", "note",
            "nothing", "notice", "number", "object", "office", "often", "okay", "once",
            "online", "only", "open", "option", "order", "other", "outside", "over",
            "page", "paper", "part", "party", "pass", "password", "path", "pattern",
            "peace", "people", "perfect", "perhaps", "period", "person", "personal", "phone",
            "photo", "pick", "picture", "piece", "place", "plan", "play", "please",
            "point", "policy", "popular", "position", "possible", "post", "power", "practice",
            "prepare", "present", "press", "pretty", "price", "print", "privacy", "private",
            "probably", "problem", "process", "produce", "product", "profile", "program", "project",
            "promise", "protect", "provide", "public", "pull", "purpose", "push", "quality",
            "question", "quick", "quickly", "quiet", "quite", "rain", "raise", "random",
            "range", "rate", "rather", "reach", "read", "ready", "real", "really",
            "reason", "receive", "recent", "record", "reduce", "reflect", "region", "regular",
            "remember", "remove", "reply", "report", "request", "require", "research", "resolve",
            "respect", "respond", "response", "rest", "result", "return", "review", "right",
            "road", "room", "round", "rule", "running", "safe", "safety", "same",
            "save", "saving", "saying", "school", "science", "screen", "search", "season",
            "second", "secret", "section", "secure", "security", "seem", "select", "send",
            "sending", "sense", "sentence", "series", "serious", "serve", "service", "session",
            "settings", "several", "share", "short", "should", "show", "side", "sign",
            "signal", "silent", "simple", "simply", "since", "single", "sister", "site",
            "size", "skill", "sleep", "small", "smart", "smile", "smooth", "social",
            "software", "solution", "some", "someone", "something", "sometimes", "soon", "sorry",
            "sort", "sound", "source", "south", "space", "speak", "special", "specific",
            "speed", "spend", "spirit", "sport", "spring", "square", "stage", "stand",
            "standard", "star", "start", "state", "statement", "station", "status", "stay",
            "step", "still", "stock", "stop", "store", "story", "straight", "street",
            "strong", "structure", "student", "study", "stuff", "style", "subject", "submit",
            "success", "such", "suddenly", "suggest", "summer", "support", "sure", "surface",
            "switch", "symbol", "system", "table", "take", "taking", "talk", "talking",
            "target", "task", "team", "tech", "technology", "tell", "term", "test",
            "text", "thank", "thanks", "that", "their", "them", "theme", "then",
            "there", "these", "they", "thing", "think", "thinking", "third", "this",
            "those", "though", "thought", "three", "through", "time", "today", "together",
            "tomorrow", "tonight", "tool", "topic", "total", "touch", "toward", "town",
            "track", "trade", "train", "travel", "tree", "trend", "trial", "true",
            "trust", "truth", "trying", "turn", "twice", "type", "typing", "under",
            "understand", "unit", "united", "until", "update", "upgrade", "upload", "upon",
            "urgent", "used", "useful", "user", "usually", "value", "various", "version",
            "very", "video", "view", "visit", "voice", "wait", "waiting", "walk",
            "want", "warm", "watch", "water", "wave", "ways", "weather", "week",
            "weekend", "welcome", "well", "went", "were", "west", "what", "whatever",
            "when", "where", "whether", "which", "while", "white", "whole", "whom",
            "whose", "wide", "wife", "will", "window", "winter", "wish", "with",
            "within", "without", "woman", "wonder", "wonderful", "word", "words", "work",
            "working", "world", "worry", "worth", "would", "write", "writing", "wrong",
            "year", "years", "yesterday", "young", "your", "yourself", "zero",
        )

        val URDU_WORDS: List<String> = listOf(
            "السلام علیکم", "وعلیکم السلام", "شکریہ", "مہربانی", "کیسے", "ہیں", "ہوں",
            "میں", "آپ", "ہم", "تم", "وہ", "یہ", "کیا", "حال", "ٹھیک", "اچھا", "بہت",
            "پاکستان", "اردو", "محبت", "دوست", "بھائی", "بہن", "گھر", "کام", "وقت",
            "آج", "کل", "صبح", "شام", "رات", "دن", "خوش", "زندگی", "دنیا", "دل",
            "خدا", "اللہ", "ان شاء اللہ", "ماشاءاللہ", "الحمدللہ", "جزاک اللہ", "دعا",
            "ہاں", "نہیں", "جی", "بالکل", "ضرور", "پانی", "کھانا", "چائے", "کتاب",
            "قلم", "علم", "سفر", "آزادی", "وطن", "لاہور", "کراچی", "اسلام آباد",
            "پشاور", "کوئٹہ", "عید", "مبارک", "سوال", "جواب", "بات", "انتظار", "مدد",
            "پیار", "امید", "روشنی", "چاند", "سورج", "بارش", "ہوا", "پھول", "سلام",
        )

        val PASHTO_WORDS: List<String> = listOf(
            "سلام", "وعلیکم سلام", "ستړي مه شئ", "په خیر راغلئ", "مننه", "مهرباني",
            "څنګه یې", "څنګه یاست", "زه", "ته", "تاسو", "موږ", "هغه", "دا", "څه",
            "ښه", "ډېر", "کور", "کار", "وخت", "نن", "سبا", "پرون", "سهار", "ماښام",
            "شپه", "ورځ", "ژوند", "نړۍ", "زړه", "ملګری", "ورور", "خور", "مور", "پلار",
            "پښتو", "افغانستان", "پېښور", "کابل", "کندهار", "کوټه", "خوشحاله",
            "دعا", "خدای", "ان شاء الله", "ماشاءاللہ", "الحمدلله", "هو", "نه", "بلکل",
            "اوبه", "ډوډۍ", "چای", "کتاب", "قلم", "پوهه", "سفر", "خپلواکي", "هېواد",
            "اختر", "مبارک", "پوښتنه", "ځواب", "خبره", "هیله", "رڼا", "سپوږمۍ", "لمر",
        )
    }
}
