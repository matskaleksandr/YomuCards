package com.QuQ.yomucards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.QuQ.yomucards.databinding.FragmentMatchingBinding

class MatchingFragment(val viewModel: TrainingViewModel) : Fragment() {
    private lateinit var binding: FragmentMatchingBinding
    private lateinit var adapterLeft: MatchingAdapter
    private lateinit var adapterRight: MatchingAdapter

    private lateinit var question: TrainingQuestion

    private var selectedLeftItem: String? = null
    private var selectedRightItem: String? = null

    private var correctAnswersCount = 0
    private var totalPairs = 0 // Общее количество пар в вопросе

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMatchingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentActivity = requireActivity() as TrainingMyCardsActivity
        question = viewModel.questions.value?.get(parentActivity.currentQuestionIndex) ?: return
        totalPairs = question.items.size // Устанавливаем общее количество пар
        setupRecyclers(question)
    }

    private fun setupRecyclers(question: TrainingQuestion?) {
        val leftItems = question?.items?.mapNotNull { it.first }?.shuffled()?.toMutableList() ?: mutableListOf()
        val rightItems = question?.items?.mapNotNull { it.second }?.shuffled()?.toMutableList() ?: mutableListOf()

        // Проверяем, есть ли пары для сопоставления
        if (leftItems.size != 3 || rightItems.size!= 3) {
            //showToast("Нет пар для сопоставления.")
            goToNextQuestion() // Переходим к следующему вопросу, если нет пар
            return
        }

        adapterLeft = MatchingAdapter(leftItems) { item ->
            selectedLeftItem = item
            checkMatch()
        }

        adapterRight = MatchingAdapter(rightItems) { item ->
            selectedRightItem = item
            checkMatch()
        }

        binding.recyclerLeft.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = adapterLeft
        }

        binding.recyclerRight.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = adapterRight
        }
    }

    private fun checkMatch() {
        if (selectedLeftItem != null && selectedRightItem != null) {
            val isCorrect = isMatchCorrect(selectedLeftItem!!, selectedRightItem!!)
            if (isCorrect) {
                correctAnswersCount++
                //showToast("Правильно! ($correctAnswersCount/$totalPairs)")

                // Удаляем правильные пары из адаптеров
                adapterLeft.removeItem(selectedLeftItem!!)
                adapterRight.removeItem(selectedRightItem!!)

                // Проверяем, достигли ли мы необходимого количества правильных ответов
                if (correctAnswersCount >= totalPairs) {
                    goToNextQuestion()
                } else {
                    clearSelection()
                }
            } else {
                //showToast("Неправильно, попробуйте ещё раз.")
                clearSelection()
            }
        }
    }

    private fun isMatchCorrect(leftItem: String, rightItem: String): Boolean {
        return question.items.any { it.first == leftItem && it.second == rightItem }
    }

    private fun goToNextQuestion() {
        val parentActivity = requireActivity() as TrainingMyCardsActivity
        parentActivity.handleAnswer(true) // Обработка правильного ответа
        correctAnswersCount = 0 // Сбросить счетчик
        parentActivity.goToNextQuestion() // Переход к следующему вопросу
    }

    private fun clearSelection() {
        selectedLeftItem = null
        selectedRightItem = null
        adapterLeft.clearSelection()
        adapterRight.clearSelection()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
