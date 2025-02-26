package com.QuQ.yomucards

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2

class TrainingMyCardsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var progressText: TextView
    private lateinit var viewModel: TrainingViewModel
    var currentQuestionIndex = 0
    private val totalQuestions = 20

    // TrainingMyCardsActivity
    fun handleAnswer(isCorrect: Boolean) {
        viewModel.questions.value?.get(currentQuestionIndex)?.isAnsweredCorrectly = isCorrect
        goToNextQuestion()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        viewPager = findViewById(R.id.viewPager)
        progressText = findViewById(R.id.progressText)

        viewModel = ViewModelProvider(this).get(TrainingViewModel::class.java)

        // Загружаем карточки и начинаем тренировку
        loadCardsAndStartTraining()
    }

    fun nextQuestion() {
        currentQuestionIndex++
        if (currentQuestionIndex < viewModel.questions.value?.size ?: 0) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MatchingFragment(viewModel))
                .commit()
        } else {
            // Все вопросы пройдены
            Toast.makeText(this, "Вы прошли все задания!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCardsAndStartTraining() {
        viewModel.loadUserCards(this).observe(this) { cards ->
            if (cards.size >= 3) {
                viewModel.generateQuestions(cards)
                setupViewPager()
            } else {
                Toast.makeText(this, "Нужно минимум 3 карточки", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupViewPager() {
        viewPager.adapter = TrainingPagerAdapter(this, viewModel)
        viewPager.isUserInputEnabled = false // Блокируем свайпы
        updateProgress()
    }

    fun goToNextQuestion() {
        if (currentQuestionIndex < totalQuestions - 1) {
            currentQuestionIndex++
            viewPager.currentItem = currentQuestionIndex
            updateProgress()
        } else {
            showResults()
        }
    }

    private fun updateProgress() {
        progressText.text = "${currentQuestionIndex + 1}/$totalQuestions"
    }

    private fun showResults() {
        // Показать результаты тренировки
        val correctAnswers = viewModel.questions.value?.count { it.isAnsweredCorrectly } ?: 0
        val resultMessage = "Правильных ответов: $correctAnswers из $totalQuestions"
        //Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
        finish()
    }
}