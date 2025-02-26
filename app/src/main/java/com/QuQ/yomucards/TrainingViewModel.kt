package com.QuQ.yomucards

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
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
                    Log.d("TrainingViewModel", "Loaded IDs for $type: $ids") // Логирование ID

                    // Получаем полные данные из SQLite
                    val kanaList = getKanaDataByIds(context, ids, type)
                    cards.addAll(kanaList)

                    Log.d("TrainingViewModel", "Loaded cards: ${cards.size}") // Логирование
                    latch.countDown() // Уменьшаем счетчик
                }
                .addOnFailureListener { e ->
                    Log.e("TrainingViewModel", "Failed to load cards: ${e.message}") // Логирование ошибки
                    latch.countDown() // Уменьшаем счетчик даже в случае ошибки
                }
        }

        // Ждем завершения всех запросов
        Thread {
            latch.await() // Ожидаем, пока счетчик не станет равным 0
            _userCards.postValue(cards) // Обновляем LiveData
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

        // Определяем имя таблицы
        val tableName = when (type) {
            "Hiragana" -> "Hiragana"
            "Katakana" -> "Katakana"
            else -> "Kanji"
        }

        // Создаем SQL-запрос в зависимости от типа таблицы
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

        // Выполняем запрос
        val cursor = db.rawQuery(query, ids.map { it.toString() }.toTypedArray())

        // Обрабатываем результат
        cursor.use {
            while (it.moveToNext()) {
                val kana = it.getString(0)
                val ENtranscription = it.getString(1)
                val RUtranscription = it.getString(2)
                val id = it.getInt(3)
                val RUtranslation = when (type) {
                    "Kanji" -> it.getString(4) // Исправлено: для Kanji берем значение из столбца RUtranslation
                    else -> null // Для Hiragana и Katakana оставляем null
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
        val questions = mutableListOf<TrainingQuestion>()
        repeat(20) {
            questions.add(generateRandomQuestion(cards))
        }
        _questions.value = questions
    }

    // Метод для проверки соответствия
    fun checkMatch(leftItem: String, rightItem: String): Boolean {
        val question = questions.value?.get(_currentQuestionIndex.value ?: 0)
        return question?.items?.any { it.first == leftItem && it.second == rightItem } ?: false
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

    // Метод для генерации случайного вопроса
    private fun generateRandomQuestion(cards: List<Kana>): TrainingQuestion {
        return when (Random.nextInt(5)) { // Увеличиваем диапазон до 9
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
            items = listOf(Pair(correctCard.pronunciation, correctCard.symbol)), // Аудио и символ
            options = options
        )
    }


    // Метод для генерации вопроса "Символ -> Транскрипция"
    private fun generateSymbolToTranscription(cards: List<Kana>): TrainingQuestion {
        val correctCard = cards.random()
        val wrongOptions = cards.filter { it != correctCard }.shuffled().take(2)
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

    // Метод для генерации вопросов соответствия
    private fun generateMatchingQuestion(cards: List<Kana>, type: QuestionType): TrainingQuestion {
        // Фильтруем карточки, чтобы оставить только те, которые подходят для типа вопроса
        val validCards = when (type) {
            QuestionType.MATCHING_TRANSCRIPTION -> cards.filter { it.symbol != null && it.pronunciation != null }
            QuestionType.MATCHING_SYMBOL -> cards.filter { it.pronunciation != null && it.symbol != null }
            else -> cards.filter { it.ruTranslation != null && it.symbol != null }
        }

        // Проверяем, что в списке достаточно карточек
        require(validCards.size >= 3) { "Недостаточно карточек для создания вопроса." }

        // Выбираем 3 случайные карточки
        val selectedCards = validCards.shuffled().take(3)

        // Создаем пары
        val items = selectedCards.map { card ->
            when (type) {
                QuestionType.MATCHING_TRANSCRIPTION -> Pair(card.symbol!!, card.pronunciation!!)
                QuestionType.MATCHING_SYMBOL -> Pair(card.pronunciation!!, card.symbol!!)
                else -> Pair(card.ruTranslation!!, card.symbol!!)
            }
        }

        // Создаем список options
        val options = items.map { it.second }

        return TrainingQuestion(
            type = type,
            items = items,
            options = options
        )
    }






    // Метод для получения текущего вопроса
    fun getCurrentQuestion(): TrainingQuestion? {
        return _questions.value?.get(_currentQuestionIndex.value ?: 0)
    }

    // Метод для сброса индекса текущего вопроса
    fun resetQuestionIndex() {
        _currentQuestionIndex.value = 0
    }

    // Метод для проверки, является ли текущий вопрос последним
    fun isLastQuestion(): Boolean {
        return _currentQuestionIndex.value == (_questions.value?.size ?: 0) - 1
    }

    // Метод для проверки, является ли текущий вопрос первым
    fun isFirstQuestion(): Boolean {
        return _currentQuestionIndex.value == 0
    }
}


