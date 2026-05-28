package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DictionaryDetail
import com.example.data.UsageSentence
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DictionaryDashboard(
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    
    // States from view model
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isEnglishInput by viewModel.isEnglishInput.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val historyList by viewModel.searchHistory.collectAsState()
    val bookmarkList by viewModel.bookmarkedWords.collectAsState()
    val notificationMsg by viewModel.notificationMessage.collectAsState()
    val dailyWord by viewModel.dailyWord.collectAsState()

    // Determine tablet landscape canonical double-column layout (Adaptive layout)
    val isTablet = configuration.screenWidthDp >= 600
    var activeTabMobile by remember { mutableIntStateOf(0) } // 0: Search & Trans, 1: Bookmarks & History, 2: Cyber-Security Settings

    // Voice Speech to Text Recognition Launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                viewModel.onSearchQueryChange(spokenText)
                viewModel.searchWord(spokenText)
                Toast.makeText(context, "ভয়েস ইনপুট: \"$spokenText\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Colors according to Sleek Interface Theme
    val slateHeaderBg = MaterialTheme.colorScheme.surface
    val slateContentBg = MaterialTheme.colorScheme.background
    val accentCoral = MaterialTheme.colorScheme.primary
    val borderSlate = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(slateContentBg)
    ) {
        // Blur content if locked
        val contentModifier = if (!isUnlocked && isBiometricEnabled) {
            Modifier.blur(16.dp).fillMaxSize()
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = contentModifier) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(accentCoral, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "অ",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Ovidhan (অভিধান)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "E-B / B-E Secured Dictionary",
                                        fontSize = 11.sp,
                                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        },
                        actions = {
                            // Notifications badge trigger
                            IconButton(
                                onClick = { 
                                    viewModel.triggerDailyNotification(context)
                                    Toast.makeText(context, "দৈনিক নোটিফিকেশন অ্যালার্ট পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("notify_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification",
                                    tint = if (isDarkTheme) Color.White else Color(0xFF334155)
                                )
                            }
                            // Theme toggler
                            IconButton(onClick = { viewModel.toggleTheme() }) {
                                Text(
                                    text = if (isDarkTheme) "☀️" else "🌙",
                                    fontSize = 20.sp
                                )
                            }
                            // Quick Lock/Auth representation
                            if (isBiometricEnabled) {
                                IconButton(onClick = { viewModel.logout() }) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock",
                                        tint = if (isDarkTheme) Color(0xFFF43F5E) else Color(0xFFE11D48)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = slateHeaderBg,
                            scrolledContainerColor = slateHeaderBg
                        )
                    )
                },
                bottomBar = {
                    if (!isTablet) {
                        // Compact mobile UI gets bottom tabs row
                        TabRow(
                            selectedTabIndex = activeTabMobile,
                            containerColor = slateHeaderBg,
                            contentColor = accentCoral,
                            indicator = @Composable { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[activeTabMobile]),
                                    color = accentCoral,
                                    height = 3.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = activeTabMobile == 0,
                                onClick = { activeTabMobile = 0 },
                                text = { Text("অভিধান খোজ", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp)) },
                                selectedContentColor = accentCoral,
                                unselectedContentColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Tab(
                                selected = activeTabMobile == 1,
                                onClick = { activeTabMobile = 1 },
                                text = { Text("সংরক্ষণ ও ইতিহাস", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = "Bookmarks", modifier = Modifier.size(20.dp)) },
                                selectedContentColor = accentCoral,
                                unselectedContentColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Tab(
                                selected = activeTabMobile == 2,
                                onClick = { activeTabMobile = 2 },
                                text = { Text("নিরাপত্তা সেটিংস", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp)) },
                                selectedContentColor = accentCoral,
                                unselectedContentColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                val layoutPadding = innerPadding

                // Notify Banner if trigger is active
                Column(
                    modifier = Modifier
                        .padding(layoutPadding)
                        .fillMaxSize()
                ) {
                    AnimatedVisibility(
                        visible = notificationMsg != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF4338CA), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alarm",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "দৈনিক শিক্ষা নোটিফিকেশন",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDarkTheme) Color(0xFFC7D2FE) else Color(0xFF312E81)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notificationMsg ?: "",
                                        fontSize = 12.sp,
                                        color = if (isDarkTheme) Color(0xFFE0E7FF) else Color(0xFF1E1B4B)
                                    )
                                }
                                IconButton(onClick = { viewModel.dismissNotificationAlert() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = if (isDarkTheme) Color(0xFFC7D2FE) else Color(0xFF312E81)
                                    )
                                }
                            }
                        }
                    }

                    // MAIN ADAPTIVE LAYOUT
                    if (isTablet) {
                        // Side-by-Side double column for Tablet Layout
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left column: Search input + Bookmarks + History + Security (Width restricted)
                            Column(
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight()
                                    .border(1.dp, borderSlate, RoundedCornerShape(0.dp))
                                    .background(slateHeaderBg)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SearchInputPanel(
                                    searchQuery = searchQuery,
                                    isEnglishInput = isEnglishInput,
                                    isDarkTheme = isDarkTheme,
                                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                                    onLangToggle = { viewModel.setEnglishInput(it) },
                                    onVoiceSearch = {
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isEnglishInput) Locale.US.toString() else "bn-BD")
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "অনুগ্রহ করে বলুন...")
                                            }
                                            voiceLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "ভয়েস ক্র্যাশ: আপনার ডিভাইসে স্পিচ রিকগনাইজার সার্ভিস নেই।", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSearchExecute = {
                                        focusManager.clearFocus()
                                        viewModel.searchWord()
                                    }
                                )

                                Divider(color = borderSlate)

                                // Quick Daily Word learning view
                                DailyWordPanel(
                                    dailyWord = dailyWord,
                                    isDarkTheme = isDarkTheme,
                                    onLoadWord = { word ->
                                        viewModel.onSearchQueryChange(word)
                                        viewModel.searchWord(word)
                                    }
                                )

                                Divider(color = borderSlate)

                                BookmarksAndHistoryTabs(
                                    historyList = historyList,
                                    bookmarkList = bookmarkList,
                                    isDarkTheme = isDarkTheme,
                                    onWordTap = { word, lang ->
                                        viewModel.setEnglishInput(lang == "en")
                                        viewModel.onSearchQueryChange(word)
                                        viewModel.searchWord(word)
                                    },
                                    onDeleteHistoryItem = { id -> viewModel.deleteHistoryItem(id) },
                                    onClearHistory = { viewModel.clearHistory() }
                                )

                                Divider(color = borderSlate)

                                SecuritySettingsPanel(
                                    isBiometricEnabled = isBiometricEnabled,
                                    onBiometricToggle = { viewModel.toggleBiometric() },
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            // Right column: Rich detail screen
                            Column(
                                modifier = Modifier
                                    .weight(0.58f)
                                    .fillMaxHeight()
                                    .padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                DictionaryDetailMainContent(
                                    uiState = uiState,
                                    isDarkTheme = isDarkTheme,
                                    viewModel = viewModel
                                )
                            }
                        }
                    } else {
                        // Standard mobile screen layout: responsive switching based on tabs
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(12.dp)
                        ) {
                            when (activeTabMobile) {
                                0 -> {
                                    // Search & Result Detail view
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        SearchInputPanel(
                                            searchQuery = searchQuery,
                                            isEnglishInput = isEnglishInput,
                                            isDarkTheme = isDarkTheme,
                                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                                            onLangToggle = { viewModel.setEnglishInput(it) },
                                            onVoiceSearch = {
                                                try {
                                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isEnglishInput) Locale.US.toString() else "bn-BD")
                                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "ধীরে ও স্পষ্ট বলুন...")
                                                    }
                                                    voiceLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "আপনার ডিভাইসে ভয়েস ইনপুট সার্ভিস পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onSearchExecute = {
                                                focusManager.clearFocus()
                                                viewModel.searchWord()
                                            }
                                        )

                                        // Fallback daily word preview if idle
                                        if (uiState is DictionaryUiState.Idle) {
                                            DailyWordPanel(
                                                dailyWord = dailyWord,
                                                isDarkTheme = isDarkTheme,
                                                onLoadWord = { word ->
                                                    viewModel.onSearchQueryChange(word)
                                                    viewModel.searchWord(word)
                                                }
                                            )
                                        }

                                        DictionaryDetailMainContent(
                                            uiState = uiState,
                                            isDarkTheme = isDarkTheme,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                                1 -> {
                                    // Bookmarks & History panels
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        BookmarksAndHistoryTabs(
                                            historyList = historyList,
                                            bookmarkList = bookmarkList,
                                            isDarkTheme = isDarkTheme,
                                            onWordTap = { word, lang ->
                                                viewModel.setEnglishInput(lang == "en")
                                                viewModel.onSearchQueryChange(word)
                                                viewModel.searchWord(word)
                                                activeTabMobile = 0 // Navigate to search
                                            },
                                            onDeleteHistoryItem = { id -> viewModel.deleteHistoryItem(id) },
                                            onClearHistory = { viewModel.clearHistory() }
                                        )
                                    }
                                }
                                2 -> {
                                    // Encryption and Biometric security panel
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SecuritySettingsPanel(
                                            isBiometricEnabled = isBiometricEnabled,
                                            onBiometricToggle = { viewModel.toggleBiometric() },
                                            isDarkTheme = isDarkTheme
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BIOMETRIC SECURE LOCK SCREEN OVERLAY (Authentic Shield)
        AnimatedVisibility(
            visible = !isUnlocked && isBiometricEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BiometricLockOverlay(
                isDarkTheme = isDarkTheme,
                onAuthenticate = { pin ->
                    // Correct PIN is 1234 or direct unlock via fingerprint
                    if (pin == "1234" || pin == "fingerprint") {
                        viewModel.authenticate(true)
                        Toast.makeText(context, "নিরাপত্তা আনলক সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "ভুল পাসকোড! পুনরায় চেষ্টা করুন। (সঠিক: ১২৩৪)", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// ---------------- SUB PANEL COMPOSABLES ----------------

@Composable
fun SearchInputPanel(
    searchQuery: String,
    isEnglishInput: Boolean,
    isDarkTheme: Boolean,
    onQueryChange: (String) -> Unit,
    onLangToggle: (Boolean) -> Unit,
    onVoiceSearch: () -> Unit,
    onSearchExecute: () -> Unit,
) {
    val activeLangColor = MaterialTheme.colorScheme.primary
    val outlineBorder = MaterialTheme.colorScheme.outline

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "অনুসন্ধান ড্যাশবোর্ড",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Dynamic Custom Toggle Switch (Segmented style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, outlineBorder, RoundedCornerShape(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // English button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isEnglishInput) activeLangColor else Color.Transparent)
                        .clickable { onLangToggle(true) }
                        .testTag("lang_en_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "English to Bangla",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isEnglishInput) MaterialTheme.colorScheme.onPrimary else (if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F))
                    )
                }

                // Bangla button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isEnglishInput) activeLangColor else Color.Transparent)
                        .clickable { onLangToggle(false) }
                        .testTag("lang_bn_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "বাংলা থেকে ইংরেজী",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (!isEnglishInput) MaterialTheme.colorScheme.onPrimary else (if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F))
                    )
                }
            }

            // Input TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_input"),
                placeholder = {
                    Text(
                        text = if (isEnglishInput) "Search English word..." else "বাংলা শব্দ লিখুন...",
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF64748B)
                                )
                            }
                        }
                        IconButton(
                            onClick = onVoiceSearch,
                            modifier = Modifier.testTag("voice_search_button")
                        ) {
                            Text(
                                text = "🎙️",
                                fontSize = 18.sp
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearchExecute() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = outlineBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )

            // Submit search block
            Button(
                onClick = onSearchExecute,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("search_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Lookup",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "অভিধান থেকে খুঁজুন",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun DailyWordPanel(
    dailyWord: DictionaryDetail?,
    isDarkTheme: Boolean,
    onLoadWord: (String) -> Unit
) {
    if (dailyWord == null) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = "আজকের দৈনিক নতুন শব্দ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "শব্দ শিখুন",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color(0xFFD0BCFF) else Color(0xFF4F378B),
                    modifier = Modifier
                        .background(
                            if (isDarkTheme) Color(0xFF4F378B).copy(alpha = 0.3f) else Color(0xFFD0BCFF).copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLoadWord(dailyWord.word) }
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = dailyWord.word.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "বাংলা অর্থ: ${dailyWord.bengaliMeaning}",
                    fontSize = 13.sp,
                    color = if (isDarkTheme) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "info",
                        tint = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF79747E),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "মনে রাখার কৌশল ও বিস্তারিত দেখতে এখানে ট্যাপ করুন।",
                        fontSize = 10.sp,
                        color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF79747E)
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksAndHistoryTabs(
    historyList: List<com.example.data.HistoryEntity>,
    bookmarkList: List<DictionaryDetail>,
    isDarkTheme: Boolean,
    onWordTap: (String, String) -> Unit,
    onDeleteHistoryItem: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    var innerTab by remember { mutableStateOf(0) } // 0: Bookmarked context, 1: History context

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toggle tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .background(if (innerTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { innerTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favs",
                        tint = if (innerTab == 0) MaterialTheme.colorScheme.onPrimary else (if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F)),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "প্রিয় শব্দ (${bookmarkList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (innerTab == 0) MaterialTheme.colorScheme.onPrimary else (if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .background(if (innerTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { innerTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏳",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ইতিহাস (${historyList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (innerTab == 1) MaterialTheme.colorScheme.onPrimary else (if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F))
                    )
                }
            }
        }

        // Contents display
        if (innerTab == 0) {
            // BOOKMARKS
            if (bookmarkList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Empty",
                            tint = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFFCAD4EA),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "কোনো প্রিয় শব্দ সংরক্ষিত নেই।",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF64748B)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bookmarkList.forEach { detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp)
                                    )
                                .clickable { onWordTap(detail.word, detail.inputLang) }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.word.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = detail.bengaliMeaning,
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Active bookmark icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                              )
                        }
                    }
                }
            }
        } else {
            // HISTORY
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "অনুসন্ধানের ইতিহাস খালি।",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF64748B)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "ইতিহাস মুছুন",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clickable { onClearHistory() }
                                .padding(4.dp)
                        )
                    }

                    historyList.forEach { history ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = history.word,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onWordTap(history.word, "en") }, // Attempt search
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { onDeleteHistoryItem(history.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF79747E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecuritySettingsPanel(
    isBiometricEnabled: Boolean,
    onBiometricToggle: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔒",
                    fontSize = 18.sp
                )
                Text(
                    text = "সাইবার এবং ডেটা নিরাপত্তা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "ব্যবহারকারীর সর্বোচ্চ ডেটা গোপনীয়তা বজায় রাখতে এই অ্যাপে লোকাল এন্ড-টু-এন্ড এনক্রিপশন ব্যবহৃত হচ্ছে। SQLite-এ সংরক্ষিত সকল শব্দ ও প্রিয় তালিকা শক্তিশালী AES-CBC দ্বারা সম্পূর্ণ এনক্রিপ্ট হয়ে অফলাইনে সেভ থাকে।",
                fontSize = 11.sp,
                color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F),
                lineHeight = 16.sp
            )

            Divider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "বায়োমেট্রিক লগইন সিস্টেম",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "সক্রিয় থাকলে অ্যাপে ঢুকতে ফিঙ্গারপ্রিন্ট লক বা ৪ সংখ্যার পিন লাগবে।",
                        fontSize = 10.sp,
                        color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF64748B)
                    )
                }

                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { onBiometricToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF79747E),
                        uncheckedTrackColor = if (isDarkTheme) Color(0xFF1C1B1F) else Color(0xFFCAD4EA)
                    )
                )
            }
        }
    }
}

// ---------------- DICTIONARY MAIN TRANSLATION CONTENT DISPLAY ----------------

@Composable
fun DictionaryDetailMainContent(
    uiState: DictionaryUiState,
    isDarkTheme: Boolean,
    viewModel: DictionaryViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            is DictionaryUiState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📚",
                        fontSize = 58.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "অভিধান অনুসন্ধান করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ভাষা সিলেক্ট করে ওপরে ইংরেজি অথবা বাংলা শব্দ খুঁজুন। প্রাক-সংরক্ষিত শব্দ খোঁজার জন্য ইন্টারনেট দরকার নেই।",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        lineHeight = 18.sp
                    )
                }
            }
            is DictionaryUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "অনুসন্ধান চলছে...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        color = Color(0xFF0E7490),
                        trackColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
            is DictionaryUiState.Success -> {
                DictionaryResultItemCard(
                    detail = uiState.detail,
                    isDarkTheme = isDarkTheme,
                    viewModel = viewModel
                )
            }
            is DictionaryUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFEF2F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "অনুসন্ধান ব্যর্থ হয়েছে",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.message,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryResultItemCard(
    detail: DictionaryDetail,
    isDarkTheme: Boolean,
    viewModel: DictionaryViewModel
) {
    val scrollState = rememberScrollState()

    // Animating dynamic components
    val bookmarkTint by animateColorAsState(
        targetValue = if (detail.isBookmarked) MaterialTheme.colorScheme.primary else (if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF79747E))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Display card: Word & Pronunciation details
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (detail.inputLang == "en") "English Entry" else "Bengali Entry",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (detail.isPhrase) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Phrase (ফ্রেজ)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }

                        if (detail.isSentence) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Sentence (বাক্য)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    // Toolbar actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pronouce primary word
                        IconButton(onClick = { viewModel.speakText(detail.word, detail.inputLang == "en") }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Pronounce",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Bookmark
                        IconButton(
                            onClick = { viewModel.toggleBookmark(detail) },
                            modifier = Modifier.testTag("bookmark_toggle_detail")
                        ) {
                            Icon(
                                imageVector = if (detail.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Bookmark Status",
                                tint = bookmarkTint
                            )
                        }
                    }
                }

                Text(
                    text = detail.word.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Divider(color = MaterialTheme.colorScheme.outline)

                // 2. Bangla Meaning & 1. English meaning
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "বাংলা অর্থ বা অনুবাদ :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF79747E)
                    )
                    Text(
                        text = detail.bengaliMeaning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (detail.englishMeaning.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "English Definition :",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF79747E)
                        )
                        Text(
                            text = detail.englishMeaning,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Detailed Explanation
        if (detail.explanation.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "বিস্তারিত ব্যাখ্যা:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = detail.explanation,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 3 & 4. Synonyms & Antonyms Panel (chips format)
        if (detail.synonyms.isNotEmpty() || detail.antonyms.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (detail.synonyms.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "সমার্থক শব্দ (Synonyms)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                helperModifiers = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detail.synonyms.forEach { syn ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.background,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = syn,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (detail.antonyms.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "বিপরীত শব্দ (Antonyms)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF43F5E)
                            )
                            FlowRow(
                                helperModifiers = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detail.antonyms.forEach { ant ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.background,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = ant,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Context Everyday Usages Sentences with Bangla meaning
        if (detail.sentences.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "দৈনন্দিন কথোপকথনে বাক্য গঠন :",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    detail.sentences.forEachIndexed { i, sentence ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${i + 1}. ${sentence.english}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.speakText(sentence.english, true) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Speak Sentence",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "অর্থ: ${sentence.bengali}",
                                fontSize = 13.sp,
                                color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F)
                            )
                        }
                    }
                }
            }
        }

        // 8. Memorization Mnemonic Trick (মনে রাখার কৌশল)
        if (detail.mnemonicTrick.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) Color(0xFF2A2438) else Color(0xFFF3EDF7)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (isDarkTheme) Color(0xFF4F378B) else Color(0xFFD0BCFF),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "শব্দটি সহজে মনে রাখার কৌশল :",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (isDarkTheme) Color(0xFFD0BCFF) else Color(0xFF381E72)
                        )
                    }

                    Text(
                        text = detail.mnemonicTrick,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (detail.mnemonicSentence.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDarkTheme) Color(0xFF1C1B1F).copy(alpha = 0.5f) else Color(0xFFE6E1E5),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "স্মৃতি সহায়ক উদাহরণ বাক্য: \"${detail.mnemonicSentence}\"",
                                fontStyle = FontStyle.Italic,
                                fontSize = 13.sp,
                                color = if (isDarkTheme) Color(0xFFD0BCFF) else Color(0xFF4F378B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 10. Grammatical Usage (গ্র্যামাটিক্যাল ব্যবহার ও নিয়ম)
        if (detail.grammaticalUsage.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = "গ্র্যামাটিক্যাল ব্যবহার ও উদাহরণ :",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = detail.grammaticalUsage,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Custom flow layout because default compose Material FlowRow can sometimes be experimental or absent
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    helperModifiers: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = helperModifiers,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

// ---------------- CYBER SECURITY BIOMETRIC SCREEN OVERLAY ----------------

@Composable
fun BiometricLockOverlay(
    isDarkTheme: Boolean,
    onAuthenticate: (String) -> Unit
) {
    var codeState by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .clickable(enabled = false) {}, // Swallow click actions
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    .clickable {
                        // Bypass via biometric touch simulation
                        onAuthenticate("fingerprint")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒",
                    fontSize = 38.sp
                )
            }

            Text(
                text = "অভিধান নিরাপত্তা শিল্ড",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "সুরক্ষার খাতিরে এই অ্যাপে বায়োমেট্রিক লগইন সক্রিয় আছে। অনুগ্রহ করে ফিঙ্গারপ্রিন্ট সেন্সরে চাপ দিন অথবা আনলক করতে ৪ সংখ্যার পিন লিখুন।\n(সিমুলেশন ব্যবহারের জন্য ওপরের তালা চিহ্নে চাপ দিতে পারেন)",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF64748B),
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value = codeState,
                onValueChange = { input ->
                    if (input.length <= 4 && input.all { it.isDigit() }) {
                        codeState = input
                    }
                },
                placeholder = { Text("৪ সংখ্যার পিন কোড...", color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onAuthenticate(codeState)
                        codeState = ""
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_code_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onAuthenticate(codeState)
                    codeState = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("unlock_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("পিন দিয়ে আনলক করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
            }

            Text(
                text = "কোম্পানির এন্ড-টু-এন্ড লোকাল এনক্রিপশন প্রটোকল দ্বারা সকল ডেটা সুরক্ষিত।",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
