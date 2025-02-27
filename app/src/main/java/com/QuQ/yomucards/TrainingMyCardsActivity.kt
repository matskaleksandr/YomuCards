    package com.QuQ.yomucards

    import android.os.Bundle
    import android.util.Log
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.lifecycle.Observer
    import androidx.lifecycle.ViewModelProvider
    import androidx.viewpager2.widget.ViewPager2
    import com.QuQ.yomucards.databinding.ActivityTrainingBinding

    class TrainingMyCardsActivity : AppCompatActivity() {
        private lateinit var binding: ActivityTrainingBinding
        private lateinit var viewModel: TrainingViewModel
        var currentQuestionIndex = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityTrainingBinding.inflate(layoutInflater)
            setContentView(binding.root)

            viewModel = ViewModelProvider(this).get(TrainingViewModel::class.java)
            setupObservers()
            loadCards()
        }

        private fun setupObservers() {
            viewModel.questions.observe(this) { questions ->
                questions?.let {
                    if (it.isNotEmpty()) showQuestion(currentQuestionIndex)
                }
            }
        }

        private fun loadCards() {
            viewModel.loadUserCards(this).observe(this) { cards ->
                if (cards.size >= 3) {
                    viewModel.generateQuestions(cards)
                } else {
                    Toast.makeText(this, "Нужно минимум 3 карточки", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        private fun showQuestion(index: Int) {
            val questionsList = viewModel.questions.value
            if (questionsList == null) {
                Log.d("QuestionUpdate", "Список вопросов пуст")
                return
            }

            // Логирование всех вопросов с их индексами
            Log.d("QuestionUpdate", "Общий список вопросов:")
            questionsList.forEachIndexed { i, question ->
                Log.d("QuestionUpdate", "Индекс: $i, Вопрос: $question")
            }

            // Логирование текущего индекса
            Log.d("QuestionUpdate", "Обновление на вопрос с индексом: $index")

            val question = questionsList.getOrNull(index) ?: return

            // Создаем фрагмент в зависимости от типа вопроса
            val fragment = when (question.type) {
                QuestionType.SYMBOL_TO_TRANSCRIPTION ->
                    SymbolToTranscriptionFragment.newInstance(index, viewModel)
                QuestionType.TRANSCRIPTION_TO_SYMBOL ->
                    TranscriptionToSymbolFragment.newInstance(index, viewModel)
                QuestionType.MATCHING_SYMBOL, QuestionType.MATCHING_TRANSCRIPTION ->
                    MatchingFragment.newInstance(index, viewModel)
                QuestionType.SOUND_TO_SYMBOL ->
                    SoundToSymbolFragment.newInstance(index, viewModel)
                else -> throw IllegalArgumentException("Unknown question type")
            }

            // Замена текущего фрагмента
            supportFragmentManager.beginTransaction()
                .replace(R.id.questionContainer, fragment)
                .commit()

            updateProgress()
        }


        fun handleAnswer(isCorrect: Boolean) {
            viewModel.questions.value?.get(currentQuestionIndex)?.isAnsweredCorrectly = isCorrect
            if (isCorrect) goToNextQuestion()
        }

        fun goToNextQuestion() {
            if (currentQuestionIndex < 19) {
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
            } else {
                showResults()
            }
        }

        private fun updateProgress() {
            binding.progressText.text = "${currentQuestionIndex + 1}/20"
        }

        private fun showResults() {
            val correct = viewModel.questions.value?.count { it.isAnsweredCorrectly } ?: 0
            Toast.makeText(this, "Правильно: $correct/20", Toast.LENGTH_LONG).show()
            finish()
        }
    }
