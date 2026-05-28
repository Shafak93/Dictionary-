package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.DictionaryDetail
import com.example.data.UsageSentence
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini request/response models ---
data class Part(val text: String)
data class Content(val parts: List<Part>)
data class GenerateContentRequest(val contents: List<Content>)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): okhttp3.ResponseBody
}

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "Request: ${request.url}")
            chain.proceed(request)
        }
        .build()

    private val service: GeminiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()))
            .build()
        retrofit.create(GeminiService::class.java)
    }

    suspend fun searchWord(word: String, isEnglishInput: Boolean): DictionaryDetail? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            Log.e(TAG, "API Key is empty or placeholder!")
            return null
        }

        val prompt = buildPrompt(word, isEnglishInput)
        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(prompt)))
            )
        )

        return try {
            val responseBody = service.generateContent(apiKey, request)
            val responseString = responseBody.string()
            Log.d(TAG, "Full JSON Response: $responseString")
            
            val jsonObject = JSONObject(responseString)
            val candidates = jsonObject.optJSONArray("candidates")
            val contentObj = candidates?.optJSONObject(0)?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""
            
            // Extract and clean JSON block from the generated text
            val jsonBlock = extractJson(rawText)
            Log.d(TAG, "Extracted JSON: $jsonBlock")
            
            parseDictionaryDetail(jsonBlock, word, if (isEnglishInput) "en" else "bn")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing API call", e)
            null
        }
    }

    private fun extractJson(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }

    private fun buildPrompt(word: String, isEnglishInput: Boolean): String {
        val formatGuide = """
        Must return ONLY a valid raw JSON object matching this exact schema:
        {
          "word": "$word",
          "inputLang": "${if (isEnglishInput) "en" else "bn"}",
          "englishMeaning": "Simple representation of the meaning in English.",
          "bengaliMeaning": "বাংলায় খুবই সহজ এবং প্রাঞ্জল অর্থ।",
          "explanation": "শব্দটির বাংলা অর্থ ও বুঝার সুবিধার্থে বিস্তারিত ব্যাখ্যা (ব্যাখ্যাটি ২-৩ লাইনে সুন্দর করে বুঝিয়ে বলবে)।",
          "synonyms": ["synonym1", "synonym2", "synonym3"],
          "antonyms": ["antonym1", "antonym2", "antonym3"],
          "sentences": [
            {
              "english": "Simple sentence using this word that is helpful in everyday conversation.",
              "bengali": "দৈনন্দিন কাজে ব্যবহৃত এই সহজ বাক্যটির বাংলা অর্থ বা অনুবাদ।"
            },
            {
              "english": "Another helpful simple conversational block or sentence.",
              "bengali": "সবচেয়ে প্রিয় ও পরিচিত ব্যবহারের বাংলা অনুবাদ।"
            }
          ],
          "isPhrase": false,
          "isSentence": false,
          "mnemonicTrick": "শব্দটি খুবই সহজে মনে রাখার একটি অসাধারণ বাস্তব ট্রিক বা কৌশল (যেমনঃ Help - আমি তোমাকে help বা সাহায্য করতে চাই)। ইংরেজি শব্দ ব্যবহারের মাধ্যমে হাস্যরসাত্মক অথবা বাস্তবমুখী বাংলা বাক্য তৈরি করো যাতে শব্দটি সহজে স্মৃতিতে গেঁথে যায়।",
          "mnemonicSentence": "মনে রাখার জন্য চূড়ান্ত উদাহরণ বাক্য যেখানে ইংরেজি শব্দটি ব্যবহার থাকবে।",
          "grammaticalUsage": "Parts of speech, grammatical rules, usage tips, and some common errors with examples. (Keep it in easy Bengali)"
        }
        """.trimIndent()

        val itemTypeCheck = """
        IMPORTANT RULES:
        1. If the input is English, parse it into Bengali and English detail in JSON.
        2. If the input is Bengali, convert/translate it to English, and provide the English base word as the primary "word" in English, explaining the translation detail in the JSON following the exact format.
        3. If the input is a PHRASE, set "isPhrase": true, and complete the detailed fields according to the rules above.
        4. If the input is a FULL SENTENCE, set "isSentence": true, translate it completely in "bengaliMeaning" and explain elements briefly in "explanation", leaving synonyms/antonyms/mnemonics empty or minimal.
        5. Double check that you return ONLY raw JSON, with no wrapping characters outside of { and }.
        """.trimIndent()

        return "Analyze the following dictionary query: \"$word\".\n\n$formatGuide\n\n$itemTypeCheck"
    }

    private fun parseDictionaryDetail(jsonStr: String, queryWord: String, currentLang: String): DictionaryDetail {
        val root = JSONObject(jsonStr)
        val word = root.optString("word", queryWord)
        val inputLang = root.optString("inputLang", currentLang)
        val englishMeaning = root.optString("englishMeaning", "")
        val bengaliMeaning = root.optString("bengaliMeaning", "")
        val explanation = root.optString("explanation", "")
        
        val synonyms = mutableListOf<String>()
        val synArray = root.optJSONArray("synonyms")
        if (synArray != null) {
            for (i in 0 until synArray.length()) {
                val syn = synArray.optString(i)
                if (syn.isNotBlank()) synonyms.add(syn)
            }
        }
        
        val antonyms = mutableListOf<String>()
        val antArray = root.optJSONArray("antonyms")
        if (antArray != null) {
            for (i in 0 until antArray.length()) {
                val ant = antArray.optString(i)
                if (ant.isNotBlank()) antonyms.add(ant)
            }
        }

        val sentences = mutableListOf<UsageSentence>()
        val sentArray = root.optJSONArray("sentences")
        if (sentArray != null) {
            for (i in 0 until sentArray.length()) {
                val sentObj = sentArray.optJSONObject(i)
                if (sentObj != null) {
                    val eng = sentObj.optString("english", "")
                    val ben = sentObj.optString("bengali", "")
                    if (eng.isNotBlank()) {
                        sentences.add(UsageSentence(eng, ben))
                    }
                }
            }
        }

        val isPhrase = root.optBoolean("isPhrase", false)
        val isSentence = root.optBoolean("isSentence", false)
        val mnemonicTrick = root.optString("mnemonicTrick", "")
        val mnemonicSentence = root.optString("mnemonicSentence", "")
        val grammaticalUsage = root.optString("grammaticalUsage", "")

        return DictionaryDetail(
            word = word,
            inputLang = inputLang,
            englishMeaning = englishMeaning,
            bengaliMeaning = bengaliMeaning,
            explanation = explanation,
            synonyms = synonyms,
            antonyms = antonyms,
            sentences = sentences,
            isPhrase = isPhrase,
            isSentence = isSentence,
            mnemonicTrick = mnemonicTrick,
            mnemonicSentence = mnemonicSentence,
            grammaticalUsage = grammaticalUsage,
            isBookmarked = false
        )
    }
}
