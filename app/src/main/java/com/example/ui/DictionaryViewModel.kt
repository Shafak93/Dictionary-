package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DictionaryDetail
import com.example.data.DictionaryRepository
import com.example.data.HistoryEntity
import com.example.data.UsageSentence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface DictionaryUiState {
    object Idle : DictionaryUiState
    object Loading : DictionaryUiState
    data class Success(val detail: DictionaryDetail) : DictionaryUiState
    data class Error(val message: String) : DictionaryUiState
}

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DictionaryViewModel"
    private val repository: DictionaryRepository
    
    // UI state states
    private val _uiState = MutableStateFlow<DictionaryUiState>(DictionaryUiState.Idle)
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    // Inputs & Theme settings
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isEnglishInput = MutableStateFlow(true)
    val isEnglishInput = _isEnglishInput.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true) // Start with premium dark mode as default
    val isDarkTheme = _isDarkTheme.asStateFlow()

    // Biometric Security States
    private val _isBiometricEnabled = MutableStateFlow(false) // Disabled by default to prevent blocking startup shields on emulators, can be enabled via settings
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false) // Controls access lock screen
    val isUnlocked = _isUnlocked.asStateFlow()

    // Notification simulator state
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage = _notificationMessage.asStateFlow()

    // Text To Speech engine holder
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // State flows from repository
    val searchHistory: StateFlow<List<HistoryEntity>>
    val bookmarkedWords: StateFlow<List<DictionaryDetail>>
    
    // Daily word state
    private val _dailyWord = MutableStateFlow<DictionaryDetail?>(null)
    val dailyWord = _dailyWord.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DictionaryRepository(database.dictionaryDao())

        // Wire up flows
        searchHistory = repository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        bookmarkedWords = repository.allBookmarked
            .map { list -> list.map { it.toDetail() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed mock data if database is empty and load daily word
        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
            loadDailyWord()
        }

        // Set initial unlock state based on biometric setting
        _isUnlocked.value = !_isBiometricEnabled.value
    }

    private fun initTts(context: Context, onReady: (() -> Unit)? = null) {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    Log.d(TAG, "TextToSpeech initialized successfully.")
                    onReady?.invoke()
                } else {
                    Log.e(TAG, "TextToSpeech initialization failed.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech engine gracefully", e)
            isTtsReady = false
        }
    }

    private fun loadDailyWord() {
        viewModelScope.launch {
            repository.allBookmarked.collect { cachedList ->
                // Collect and pick one for today's dynamic word
                if (cachedList.isNotEmpty()) {
                    val index = ((System.currentTimeMillis() / (1000 * 60 * 60 * 24)) % cachedList.size).toInt()
                    val entity = cachedList.getOrNull(index) ?: cachedList.first()
                    _dailyWord.value = entity.toDetail()
                } else {
                    // Fallback to default
                    _dailyWord.value = DictionaryDetail(
                        word = "resilient",
                        inputLang = "en",
                        englishMeaning = "Able to recover quickly from difficult conditions.",
                        bengaliMeaning = "সহনশীল, ঘুরে দাঁড়াতে সক্ষম।",
                        explanation = "যা যে কোনো বিপদ বা বাধা অতিক্রম করে আবার আগের অবস্থায় স্বাভাবিক হয়ে যেতে পারে বা জয় লাভ করে।",
                        synonyms = listOf("tough", "strong"),
                        antonyms = listOf("fragile", "vulnerable"),
                        sentences = listOf(UsageSentence("Bengali people are resilient.", "বাঙালিরা অনেক সহনশীল।")),
                        isPhrase = false,
                        isSentence = false,
                        mnemonicTrick = "রেজিলিয়েন্ট মানে 'রোজ লড়াকু'!",
                        mnemonicSentence = "আমরা Resilient জাতি।",
                        grammaticalUsage = "Adjective"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setEnglishInput(isEnglish: Boolean) {
        _isEnglishInput.value = isEnglish
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleBiometric() {
        _isBiometricEnabled.value = !_isBiometricEnabled.value
        if (!_isBiometricEnabled.value) {
            _isUnlocked.value = true
        }
    }

    fun authenticate(success: Boolean) {
        if (success) {
            _isUnlocked.value = true
        }
    }

    fun logout() {
        if (_isBiometricEnabled.value) {
            _isUnlocked.value = false
        }
    }

    fun searchWord(queryStr: String = _searchQuery.value) {
        val finalQuery = queryStr.trim()
        if (finalQuery.isEmpty()) return

        _searchQuery.value = finalQuery
        _uiState.value = DictionaryUiState.Loading

        viewModelScope.launch {
            val result = repository.getCachedOrFetch(finalQuery, _isEnglishInput.value, getApplication())
            result.onSuccess { detail ->
                _uiState.value = DictionaryUiState.Success(detail)
            }
            result.onFailure { error ->
                _uiState.value = DictionaryUiState.Error(error.message ?: "একটি অপরিচিত সমস্যা হয়েছে।")
            }
        }
    }

    fun toggleBookmark(detail: DictionaryDetail) {
        viewModelScope.launch {
            repository.toggleBookmark(detail.word, detail.isBookmarked)
            // Re-sync success state visually
            val state = _uiState.value
            if (state is DictionaryUiState.Success && state.detail.word == detail.word) {
                _uiState.value = DictionaryUiState.Success(state.detail.copy(isBookmarked = !detail.isBookmarked))
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Pronouce word / sentence vocally via built-in TTS (Offline support)
    fun speakText(text: String, isEnglish: Boolean) {
        if (!isTtsReady || tts == null) {
            initTts(getApplication()) {
                executeSpeak(text, isEnglish)
            }
        } else {
            executeSpeak(text, isEnglish)
        }
    }

    private fun executeSpeak(text: String, isEnglish: Boolean) {
        try {
            val result = if (isEnglish) {
                tts?.setLanguage(Locale.US)
            } else {
                // Bengali voice locale setup
                tts?.setLanguage(Locale("bn", "BD")) ?: tts?.setLanguage(Locale("bn", "IN"))
            }

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Audio language pack not loaded or not supported on this emulator device.")
                // Attempt to speak with default anyway
                tts?.setLanguage(Locale.US)
            }

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "OvidhanSpeechId")
        } catch (e: Exception) {
            Log.e(TAG, "Speech execution failed", e)
        }
    }

    // Send daily word learning notification internally
    fun triggerDailyNotification(context: Context) {
        val wordName = _dailyWord.value?.word ?: "Resilient"
        val wordMeaning = _dailyWord.value?.bengaliMeaning ?: "সহনশীল"
        
        _notificationMessage.value = "আজকের নতুন শব্দ: \"$wordName\" - যার বাংলা অর্থ হলো \"$wordMeaning\"। শব্দটির বিস্তারিত মনে রাখার ট্রিক জানতে ট্যাপ করুন!"
        
        // Also simulate Android standard notification
        sendSystemNotification(context, wordName, wordMeaning)
    }

    private fun sendSystemNotification(context: Context, word: String, meaning: String) {
        try {
            // Check for POST_NOTIFICATIONS permission on Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Cannot post system notification: POST_NOTIFICATIONS permission not granted.")
                    return
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "ovidhan_daily_word_channel"
            
            // Register channel on Oreo/higher
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channelName = "Daily Vocabulary Learning"
                val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
                val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                    description = "Daily notifications to learn new concepts dynamically."
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Simple notification object
            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Ovidhan (অভিধান) - দৈনিক শব্দ")
                .setContentText("আজকের শব্দ: $word ($meaning)")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(4829, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Could not post system notification", e)
        }
    }

    fun dismissNotificationAlert() {
        _notificationMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up TTS", e)
        }
    }
}
