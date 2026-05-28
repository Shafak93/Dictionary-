package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DictionaryRepository(
    private val dao: DictionaryDao
) {
    private val TAG = "DictionaryRepository"

    val allBookmarked: Flow<List<DictionaryEntity>> = dao.getBookmarkedWordsFlow()
    val allHistory: Flow<List<HistoryEntity>> = dao.getHistoryFlow()
    val allCached: Flow<List<DictionaryEntity>> = dao.getAllCachedWordsFlow()

    suspend fun getCachedOrFetch(wordStr: String, isEnglishInput: Boolean, context: Context): Result<DictionaryDetail> = withContext(Dispatchers.IO) {
        val wordQuery = wordStr.trim().lowercase()
        if (wordQuery.isEmpty()) {
            return@withContext Result.failure(Exception("খালি শব্দ খোঁজা সম্ভব নয়। অনুগ্রহ করে শব্দ লিখুন।"))
        }

        // Add to history
        try {
            dao.deleteHistoryByWord(wordQuery)
            dao.insertHistory(HistoryEntity(word = wordQuery))
        } catch (e: Exception) {
            Log.e(TAG, "History insertion error", e)
        }

        // 1. Try to find the word in Local decrypted / encrypted cache
        val localCached = dao.getCachedWord(wordQuery)
        if (localCached != null) {
            Log.d(TAG, "Cached item hit! Loading from database.")
            // Update timestamp so it becomes active or stays in cache
            val updated = localCached.copy(timestamp = System.currentTimeMillis())
            dao.insertCachedWord(updated)
            return@withContext Result.success(updated.toDetail())
        }

        // 2. Not in local cache, let's try to query Gemini API
        Log.d(TAG, "Local cache miss. Querying Gemini REST API.")
        try {
            val fetchedDetail = GeminiApiClient.searchWord(wordQuery, isEnglishInput)
            if (fetchedDetail != null) {
                // Save to local cache
                val entityToCache = DictionaryEntity.fromDetail(fetchedDetail)
                dao.insertCachedWord(entityToCache)
                return@withContext Result.success(fetchedDetail)
            } else {
                return@withContext Result.failure(Exception("Gemini API থেকে অনুবাদ পাওয়া যায়নি। অনুগ্রহ করে ইন্টারনেট সংযোগ চেক করুন।"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API failed", e)
            return@withContext Result.failure(
                Exception("অনুবাদে ব্যর্থতা ঘটেছে। ডিভাইসটি অফলাইনে থাকতে পারে। আপনি প্রাক-সংরক্ষিত শব্দগুলি খুঁজতে পারেন। ${e.localizedMessage}")
            )
        }
    }

    suspend fun toggleBookmark(wordStr: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        val wordKey = wordStr.trim().lowercase()
        dao.updateBookmarkStatus(wordKey, !currentStatus)
    }

    suspend fun deleteHistoryItem(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteHistoryItem(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }

    // High quality preloaded seed data to give the app perfect offline utility instantly!
    suspend fun seedMockDataIfEmpty() = withContext(Dispatchers.IO) {
        val totalCached = dao.getCachedWord("help")
        if (totalCached == null) {
            Log.d(TAG, "Pre-seeding Ovidhan with high density English and Bengali dictionaries")
            val seeds = listOf(
                DictionaryDetail(
                    word = "help",
                    inputLang = "en",
                    englishMeaning = "To assist, support or give aid to someone.",
                    bengaliMeaning = "সাহায্য করা, সহায়তা করা।",
                    explanation = "কাউকে কোনো কাজে সাপোর্ট করা বা সাহায্য প্রদান করাকে help বলা হয়। এটি একটি অত্যন্ত পরিচিত এবং দরকারী দৈনন্দিন শব্দ।",
                    synonyms = listOf("assist", "support", "aid", "succor"),
                    antonyms = listOf("hinder", "obstruct", "harm", "oppose"),
                    sentences = listOf(
                        UsageSentence("Can you help me with this heavy box?", "আপনি কি এই ভারী বাক্সটি সরাতে আমাকে সাহায্য করতে পারেন?"),
                        UsageSentence("We should always help people in need.", "আমাদের সর্বদা অভাবী মানুষদের সাহায্য করা উচিত।")
                    ),
                    isPhrase = false,
                    isSentence = false,
                    mnemonicTrick = "Help - আমি তোমাকে help করতে চাই। কঠিন সময়ে বন্ধুদের 'হেল্প' করার মাধ্যমে সম্পর্ক মধুর হয়।",
                    mnemonicSentence = "আমি তোমাকে Help করতে চাই।",
                    grammaticalUsage = "Help একটি Verb এবং Noun হিসেবে ব্যবহৃত হয়। যেমন- (Verb) Please help me. (Noun) She cried out for help."
                ),
                DictionaryDetail(
                    word = "beautiful",
                    inputLang = "en",
                    englishMeaning = "Pleasing the senses or mind aesthetically.",
                    bengaliMeaning = "সুন্দর, চমৎকার, মনোহর।",
                    explanation = "যা দেখতে আকর্ষণীয়, চোখ জুড়ানো এবং মনের আনন্দ বাড়ায় তাকে beautiful বলা হয়। এটি মানুষ, মনোহর দৃশ্য এবং কাজের ক্ষেত্রে ব্যবহার করা চলে।",
                    synonyms = listOf("pretty", "lovely", "gorgeous", "handsome"),
                    antonyms = listOf("ugly", "hideous", "unattractive"),
                    sentences = listOf(
                        UsageSentence("The sunset looks extremely beautiful today.", "আজ সূর্যাস্ত দেখতে অত্যন্ত সুন্দর লাগছে।"),
                        UsageSentence("She has a beautiful smile.", "তার একটি সুন্দর হাসি আছে।")
                    ),
                    isPhrase = false,
                    isSentence = false,
                    mnemonicTrick = "Beautiful - বিউটিফুল মেকআপ ছাড়াই তুমি অনেক beautiful বা সুন্দর। মনের সৌন্দর্যই মানুষের আসল পরিচয়।",
                    mnemonicSentence = "কুয়াশাঘেরা সকালটি আসলেই অনেক Beautiful।",
                    grammaticalUsage = "Beautiful একটি Adjective। যা কোনো Noun এর গুণ প্রকাশ করে। যেমন: It is a beautiful flower."
                ),
                DictionaryDetail(
                    word = "resilient",
                    inputLang = "en",
                    englishMeaning = "Able to withstand or recover quickly from difficult conditions.",
                    bengaliMeaning = "সহনশীল, স্থিতিস্থাপক, ঘুরে দাঁড়ানোর ক্ষমতাসম্পন্ন।",
                    explanation = "বিপদ বা কঠিন পরিস্থিতি থেকে দ্রুত আগের অবস্থানে ফিরে আসার অদম্য মানসিক বা শারীরিক শক্তিকে resilient বলা হয়।",
                    synonyms = listOf("tough", "flexible", "hardy", "strong"),
                    antonyms = listOf("fragile", "weak", "sensitive", "vulnerable"),
                    sentences = listOf(
                        UsageSentence("Our people are highly resilient in facing natural disasters.", "প্রাকৃতিক দুর্যোগ মোকাবেলায় আমাদের দেশের মানুষেরা অত্যন্ত সহনশীল ও ঘুরে দাঁড়াতে সক্ষম।"),
                        UsageSentence("A resilient mindset helps you face struggles boldly.", "একটি ঘুরে দাঁড়ানোর শক্তিসম্পন্ন মানসিকতা আপনাকে লড়াই করতে সাহায্য করে।")
                    ),
                    isPhrase = false,
                    isSentence = false,
                    mnemonicTrick = "Resilient - বাংলায় রেজিলিয়েন্ট বা 'রোজ লড়াকু'। যে ব্যক্তি রোজ জীবনের শত ঝড়েও লড়তে পারে সে-ই resilient।",
                    mnemonicSentence = "বাঙালি জাতি যুদ্ধের ইতিহাসে প্রচণ্ড Resilient হিসেবে পরিচিত।",
                    grammaticalUsage = "Resilient একটি Adjective। Noun রূপ resilience। যেমন: He has a resilient character."
                ),
                DictionaryDetail(
                    word = "sovereign",
                    inputLang = "en",
                    englishMeaning = "Possessing supreme or ultimate power; fully independent.",
                    bengaliMeaning = "সার্বভৌম, সর্বোচ্চ ক্ষমতার অধিকারী।",
                    explanation = "যে রাষ্ট্র বা সংস্থা বাইরের কোনো শক্তি দ্বারা নিয়ন্ত্রিত নয় এবং নিজের সম্পূর্ণ ক্ষমতা বজায় রাখে, তাকে sovereign বা স্বাধীন দেশ বলা হয়।",
                    synonyms = listOf("independent", "supreme", "autonomous", "free"),
                    antonyms = listOf("dependent", "subjugated", "colonized"),
                    sentences = listOf(
                        UsageSentence("Bangladesh became a sovereign nation in 1971.", "১৯৭১ সালে বাংলাদেশ একটি স্বাধীন সার্বভৌম রাষ্ট্র হিসেবে প্রতিষ্ঠিত হয়।"),
                        UsageSentence("The ultimate authority lies with the sovereign power of the country.", "চূড়ান্ত কর্তৃত্ব দেশের সার্বভৌম ক্ষমতার ওপর ন্যস্ত থাকে।")
                    ),
                    isPhrase = false,
                    isSentence = false,
                    mnemonicTrick = "Sovereign - 'সব রান'-এর মালিক যে দেশ অর্থাৎ যে নিজেই রাজা এবং নিজের স্বাধীনতায় খেলে, সে-ই sovereign।",
                    mnemonicSentence = "সার্বভৌম রাষ্ট্র হিসেবে প্রতিটি নাগরিকের স্বাধীনভাবে বেঁচে থাকার Sovereign অধিকার আছে।",
                    grammaticalUsage = "Sovereign একটি Adjective এবং Noun। যেমন- (Adj) This is a sovereign country. (Noun) The king is the sovereign of the kingdom."
                ),
                DictionaryDetail(
                    word = "অনুপ্রেরণা",
                    inputLang = "bn",
                    englishMeaning = "Inspiration or motivation to achieve goals.",
                    bengaliMeaning = "Inspiration (অনুপ্রেরণা)।",
                    explanation = "কোনো কাজ করতে বা লক্ষ্য অর্জনে মনের ভেতর তৈরি হওয়া অদম্য উদ্দীপনা বা উৎসাহই হলো অনুপ্রেরণা বা Inspiration।",
                    synonyms = listOf("inspiration", "motivation", "encouragement", "stimulus"),
                    antonyms = listOf("discouragement", "demotivation", "hindrance"),
                    sentences = listOf(
                        UsageSentence("My parents are my greatest inspiration.", "আমার বাবা-মা আমার সবচেয়ে বড় অনুপ্রেরণা।"),
                        UsageSentence("A good teacher gives inspiration to design a bright future.", "একজন ভালো শিক্ষক একটি উজ্জ্বল ভবিষ্যৎ গড়ার অনুপ্রেরণা যোগান।")
                    ),
                    isPhrase = false,
                    isSentence = false,
                    mnemonicTrick = "अनुপ্রেরণা - ইংরেজিতে Inspiration। তোমার ইন-স্পিরিট বা ভেতরের আত্মাকে জাগ্রত করে যে অনুপ্রেরণা বা Inspiration।",
                    mnemonicSentence = "সন্তানের সফলতায় মায়ের অবদান সবচেয়ে বড় Inspiration বা অনুপ্রেরণা।",
                    grammaticalUsage = "অনুপ্রেরণা বা Inspiration একটি Noun। verb রূপ হলো inspire। যেমন: She inspired me to write this dictionary app."
                ),
                DictionaryDetail(
                    word = "ধন্যবাদ",
                    inputLang = "bn",
                    englishMeaning = "Thank you; expressing gratitude and appreciation.",
                    bengaliMeaning = "Thank you / Gratitude (ধন্যবাদ বা কৃতজ্ঞতা প্রকাশ)।",
                    explanation = "কারো কোনো উপকার বা চমৎকার কাজের পরে সম্মান বা কৃতজ্ঞতা জানানোর সহজতম সামাজিক অভিব্যক্তি হলো ধন্যবাদ।",
                    synonyms = listOf("thanks", "gratitude", "appreciation", "gratefulness"),
                    antonyms = listOf("rudeness", "ingratitude"),
                    sentences = listOf(
                        UsageSentence("Thank you so much for your kind assistance.", "আপনার সদয় সহায়তার জন্য আপনাকে অনেক অনেক ধন্যবাদ।"),
                        UsageSentence("We should express gratitude for everyone's hard work.", "সবার কঠোর পরিশ্রমের জন্য আমাদের কৃতজ্ঞতা প্রকাশ বা ধন্যবাদ জানানো উচিত।")
                    ),
                    isPhrase = false,
                    isSentence = false,
                    mnemonicTrick = "ধন্যবাদ - মুখে সর্বদা 'থ্যাংক ইউ' বা ধন্যবাদ বললে চারপাশের মানুষের সাথে সুসম্পর্ক গড়ে ওঠে।",
                    mnemonicSentence = "ধন্যবাদ বা Thanks জানানো একটি অত্যন্ত মহৎ এবং সভ্য গুণ।",
                    grammaticalUsage = "ধন্যবাদ বা Thanks একটি Interjection, Noun বা Verb হিসেবে কাজ করে। যেমন: (Interj) Thank you! (Noun) Offer thanks to Almighty."
                )
            )

            for (seed in seeds) {
                dao.insertCachedWord(DictionaryEntity.fromDetail(seed))
            }
        }
    }
}
