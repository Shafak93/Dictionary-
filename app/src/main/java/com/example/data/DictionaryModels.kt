package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.CryptoManager

@Entity(tableName = "cached_words")
data class DictionaryEntity(
    @PrimaryKey val word: String, // Keep the raw word as key (plain text or encrypted - plain text is better for search/contains matching)
    val inputLang: String, // "en" or "bn"
    val englishMeaningEncrypted: String,
    val bengaliMeaningEncrypted: String,
    val explanationEncrypted: String,
    val synonymsEncrypted: String, // comma separated encrypted
    val antonymsEncrypted: String, // comma separated encrypted
    val sentencesEncrypted: String, // JSON list or pipe separated encrypted
    val isPhrase: Boolean,
    val isSentence: Boolean,
    val mnemonicTrickEncrypted: String,
    val mnemonicSentenceEncrypted: String,
    val grammaticalUsageEncrypted: String,
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Decrypted helper mapping to make operations super clean
    fun toDetail(): DictionaryDetail {
        return DictionaryDetail(
            word = word,
            inputLang = inputLang,
            englishMeaning = CryptoManager.decrypt(englishMeaningEncrypted),
            bengaliMeaning = CryptoManager.decrypt(bengaliMeaningEncrypted),
            explanation = CryptoManager.decrypt(explanationEncrypted),
            synonyms = CryptoManager.decrypt(synonymsEncrypted).split(",").filter { it.isNotBlank() },
            antonyms = CryptoManager.decrypt(antonymsEncrypted).split(",").filter { it.isNotBlank() },
            sentences = try {
                val decrypted = CryptoManager.decrypt(sentencesEncrypted)
                decrypted.split("||").filter { it.isNotBlank() }.map { s ->
                    val parts = s.split(">>")
                    if (parts.size >= 2) {
                        UsageSentence(parts[0], parts[1])
                    } else {
                        UsageSentence(s, "")
                    }
                }
            } catch (e: Exception) {
                emptyList()
            },
            isPhrase = isPhrase,
            isSentence = isSentence,
            mnemonicTrick = CryptoManager.decrypt(mnemonicTrickEncrypted),
            mnemonicSentence = CryptoManager.decrypt(mnemonicSentenceEncrypted),
            grammaticalUsage = CryptoManager.decrypt(grammaticalUsageEncrypted),
            isBookmarked = isBookmarked
        )
    }

    companion object {
        fun fromDetail(detail: DictionaryDetail): DictionaryEntity {
            val sentencesSerialized = detail.sentences.joinToString("||") { "${it.english}>>${it.bengali}" }
            return DictionaryEntity(
                word = detail.word.trim().lowercase(),
                inputLang = detail.inputLang,
                englishMeaningEncrypted = CryptoManager.encrypt(detail.englishMeaning),
                bengaliMeaningEncrypted = CryptoManager.encrypt(detail.bengaliMeaning),
                explanationEncrypted = CryptoManager.encrypt(detail.explanation),
                synonymsEncrypted = CryptoManager.encrypt(detail.synonyms.joinToString(",")),
                antonymsEncrypted = CryptoManager.encrypt(detail.antonyms.joinToString(",")),
                sentencesEncrypted = CryptoManager.encrypt(sentencesSerialized),
                isPhrase = detail.isPhrase,
                isSentence = detail.isSentence,
                mnemonicTrickEncrypted = CryptoManager.encrypt(detail.mnemonicTrick),
                mnemonicSentenceEncrypted = CryptoManager.encrypt(detail.mnemonicSentence),
                grammaticalUsageEncrypted = CryptoManager.encrypt(detail.grammaticalUsage),
                isBookmarked = detail.isBookmarked
            )
        }
    }
}

@Entity(tableName = "search_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DictionaryDetail(
    val word: String,
    val inputLang: String,
    val englishMeaning: String,
    val bengaliMeaning: String,
    val explanation: String,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val sentences: List<UsageSentence>,
    val isPhrase: Boolean,
    val isSentence: Boolean,
    val mnemonicTrick: String,
    val mnemonicSentence: String,
    val grammaticalUsage: String,
    val isBookmarked: Boolean = false
)

data class UsageSentence(
    val english: String,
    val bengali: String
)
