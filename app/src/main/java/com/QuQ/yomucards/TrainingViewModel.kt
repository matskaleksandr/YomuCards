package com.QuQ.yomucards

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.File
import kotlin.random.Random
import java.util.concurrent.CountDownLatch

class TrainingViewModel : ViewModel() {

    private val _userCards = MutableLiveData<List<Kana>>()
    val userCards: LiveData<List<Kana>> = _userCards

    private val _questions = MutableLiveData<List<TrainingQuestion>>()
    val questions: LiveData<List<TrainingQuestion>> = _questions

    private val _currentQuestionIndex = MutableLiveData<Int>(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex

    private val usedCards = mutableSetOf<Kana>() // Храним использованные карточки

    // Метод для загрузки карточек пользователя
    fun loadUserCards(context: Context): LiveData<List<Kana>> {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return _userCards

        val database = Firebase.database.reference
        val userId = currentUser.uid

        val cards = mutableListOf<Kana>()
        val latch = CountDownLatch(3) // Ожидаем завершения 3 запросов (Hiragana, Katakana, Kanji)

        listOf("Hiragana", "Katakana", "Kanji").forEach { type ->
            database.child("Users/$userId/Stats_YomuCards/Cards/$type").get()
                .addOnSuccessListener { snapshot ->
                    val ids = snapshot.children.mapNotNull { it.key?.toInt() }
                    Log.d("TrainingViewModel", "Loaded IDs for $type: $ids")

                    // Получаем полные данные из SQLite
                    val kanaList = getKanaDataByIds(context, ids, type)
                    cards.addAll(kanaList)

                    Log.d("TrainingViewModel", "Loaded cards: ${cards.size}")
                    latch.countDown()
                }
                .addOnFailureListener { e ->
                    Log.e("TrainingViewModel", "Failed to load cards: ${e.message}")
                    latch.countDown()
                }
        }

        // Ждем завершения всех запросов
        Thread {
            latch.await()
            _userCards.postValue(cards)
        }.start()

        return _userCards
    }

    // Метод для получения базы данных
    private fun getDatabase(context: Context): SQLiteDatabase {
        val databasePath = File(context.filesDir, "yomucardsdb.db").absolutePath
        return SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    // Метод для получения данных Kana по ID и типу
    private fun getKanaDataByIds(context: Context, ids: List<Int>, type: String): List<Kana> {
        val kanaList = mutableListOf<Kana>()
        val db = getDatabase(context)

        val tableName = when (type) {
            "Hiragana" -> "Hiragana"
            "Katakana" -> "Katakana"
            else -> "Kanji"
        }

        // Создаем строку с плейсхолдерами ? для каждого ID
        val placeholders = ids.joinToString(",") { "?" }
        val query = when (type) {
            "Kanji" -> """
            SELECT kanj, ENtranscription, RUtranscription, RUtranslation, ID 
            FROM $tableName 
            WHERE ID IN ($placeholders)
        """
            else -> """
            SELECT kana, ENtranscription, RUtranscription, ID 
            FROM $tableName 
            WHERE ID IN ($placeholders)
        """
        }

        // Преобразуем IDs в массив строк для привязки
        val idStrings = ids.map { it.toString() }.toTypedArray()

        // Выполняем запрос с IDs в качестве аргументов
        val cursor = db.rawQuery(query, idStrings)

        // Обрабатываем результат
        cursor.use {
            while (it.moveToNext()) {
                val kana = it.getString(0)
                val ENtranscription = it.getString(1)
                val RUtranscription = it.getString(2)
                val id = it.getInt(3)
                val RUtranslation = when (type) {
                    "Kanji" -> it.getString(4)
                    else -> null
                }
                val kanaType = when (type) {
                    "Hiragana" -> KanaType.HIRAGANA
                    "Katakana" -> KanaType.KATAKANA
                    else -> KanaType.KANJI
                }
                kanaList.add(Kana(id, kana, ENtranscription, RUtranscription, RUtranslation, kanaType))
                Log.d("getKanaDataByIds", "Fetched: $kana, $ENtranscription, $RUtranscription, $RUtranslation, $id")
            }
        }

        // Закрываем базу данных
        db.close()
        return kanaList
    }

    // Метод для генерации вопросов
    fun generateQuestions(cards: List<Kana>) {
        val newQuestions = mutableListOf<TrainingQuestion>()
        repeat(20) {
            newQuestions.add(generateRandomQuestion(cards))
        }
        Log.d("ВОПРОСЫ", "$newQuestions")
        _questions.value = newQuestions
    }

    // Метод для генерации случайного вопроса
    private fun generateRandomQuestion(cards: List<Kana>): TrainingQuestion {
        return when (Random.nextInt(5)) {
            0 -> generateSymbolToTranscription(cards)
            1 -> generateTranscriptionToSymbol(cards)
            2 -> generateMatchingQuestion(cards, QuestionType.MATCHING_TRANSCRIPTION)
            3 -> generateMatchingQuestion(cards, QuestionType.MATCHING_SYMBOL)
            4 -> generateSoundToSymbolQuestion(cards)
            else -> generateMatchingQuestion(cards, QuestionType.MATCHING_SYMBOL)
        }
    }

    private fun generateSoundToSymbolQuestion(cards: List<Kana>): TrainingQuestion {
        val correctCard = cards.random()
        val wrongOptions = cards.filter { it != correctCard }.shuffled().take(2)
        val options = (listOf(correctCard.symbol) + wrongOptions.map { it.symbol }).shuffled()

        return TrainingQuestion(
            type = QuestionType.SOUND_TO_SYMBOL,
            items = listOf(Pair(correctCard.symbol, correctCard.symbol)),
            options = options
        )
    }

    // Метод для генерации вопроса "Символ -> Транскрипция"
    private fun generateSymbolToTranscription(cards: List<Kana>): TrainingQuestion {
        val availableCards = cards.filter { it !in usedCards }

        if (availableCards.size < 3) {
            usedCards.clear() // Очищаем использованные карточки, если доступных недостаточно
            return generateSymbolToTranscription(cards)
        }

        val correctCard = availableCards.random()
        usedCards.add(correctCard)

        val wrongOptions = availableCards.filter { it != correctCard }.shuffled().take(2)
        val options = (listOf(correctCard.pronunciation) + wrongOptions.map { it.pronunciation }).shuffled()

        return TrainingQuestion(
            type = QuestionType.SYMBOL_TO_TRANSCRIPTION,
            items = listOf(Pair(correctCard.symbol, correctCard.pronunciation)),
            options = options
        )
    }

    // Метод для генерации вопроса "Транскрипция -> Символ"
    private fun generateTranscriptionToSymbol(cards: List<Kana>): TrainingQuestion {
        val correctCard = cards.random()
        val wrongOptions = cards.filter { it != correctCard }.shuffled().take(2)
        val options = (listOf(correctCard.symbol) + wrongOptions.map { it.symbol }).shuffled()

        return TrainingQuestion(
            type = QuestionType.TRANSCRIPTION_TO_SYMBOL,
            items = listOf(Pair(correctCard.pronunciation, correctCard.symbol)),
            options = options
        )
    }

    // Метод для генерации вопроса на сопоставление
    private fun generateMatchingQuestion(
        cards: List<Kana>,
        questionType: QuestionType
    ): TrainingQuestion {
        val availableCards = cards.filter { it !in usedCards }

        if (availableCards.size < 3) {
            usedCards.clear() // Очищаем использованные карточки, если доступных недостаточно
            return generateMatchingQuestion(cards, questionType)
        }

        val selectedCards = availableCards.shuffled().take(3)
        val correctPairs = selectedCards.map { card ->
            when (questionType) {
                QuestionType.MATCHING_SYMBOL -> Pair(card.symbol, card.pronunciation)
                QuestionType.MATCHING_TRANSCRIPTION -> Pair(card.pronunciation, card.symbol)
                else -> Pair(card.symbol, card.pronunciation)
            }
        }

        // Добавляем выбранные карточки в usedCards
        usedCards.addAll(selectedCards)

        return TrainingQuestion(
            type = questionType,
            items = correctPairs,
            options = correctPairs.map { it.second }.shuffled()
        )
    }


    // Метод для перехода к следующему вопросу
    fun nextQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex < (_questions.value?.size ?: 0) - 1) {
            _currentQuestionIndex.value = currentIndex + 1
        }
    }

    // Метод для перехода к предыдущему вопросу
    fun previousQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        if (currentIndex > 0) {
            _currentQuestionIndex.value = currentIndex - 1
        }
    }

    // Метод для сброса индекса текущего вопроса
    fun resetQuestionIndex() {
        _currentQuestionIndex.value = 0
    }
}




