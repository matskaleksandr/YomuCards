package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.QuQ.yomucards.databinding.FragmentMatchingBinding

class MatchingFragment(private val viewModel: TrainingViewModel) : Fragment() {

    private lateinit var binding: FragmentMatchingBinding
    private lateinit var adapterLeft: MatchingAdapter
    private lateinit var adapterRight: MatchingAdapter

    private lateinit var question: TrainingQuestion

    private var currentQuestionIndex: Int = 0

    private var selectedLeftItem: String? = null
    private var selectedRightItem: String? = null

    private var correctAnswersCount = 0
    private var totalPairs = 0 // Общее количество пар в вопросе

    companion object {
        private const val ARG_QUESTION_INDEX = "question_index"
        fun newInstance(questionIndex: Int, viewModel: TrainingViewModel): MatchingFragment {
            val fragment = MatchingFragment(viewModel)
            val args = Bundle()
            args.putInt(ARG_QUESTION_INDEX, questionIndex)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentQuestionIndex = arguments?.getInt(ARG_QUESTION_INDEX) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMatchingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Используем локальный currentQuestionIndex, а не значение из родительской активности
        question = viewModel.questions.value?.getOrNull(currentQuestionIndex) ?: return
        totalPairs = question.items.size // Устанавливаем общее количество пар
        setupRecyclers(question)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclers(question: TrainingQuestion) {
        val leftItems = question.items.mapNotNull { it.first }.shuffled().toMutableList()
        val rightItems = question.items.mapNotNull { it.second }.shuffled().toMutableList()

        Log.d("setupRecyclers", "Left items: $leftItems")
        Log.d("setupRecyclers", "Right items: $rightItems")

        if (leftItems.size != totalPairs || rightItems.size != totalPairs) {
            Log.e("setupRecyclers", "Недостаточно пар для сопоставления.")
            showToast("Недостаточно данных для задания.")
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

        binding.recyclerLeft.layoutManager = LinearLayoutManager(context)
        binding.recyclerRight.layoutManager = LinearLayoutManager(context)

        binding.recyclerLeft.adapter = adapterLeft
        binding.recyclerRight.adapter = adapterRight

        adapterLeft.notifyDataSetChanged()
        adapterRight.notifyDataSetChanged()
    }

    private fun checkMatch() {
        if (selectedLeftItem != null && selectedRightItem != null) {
            val isCorrect = isMatchCorrect(selectedLeftItem!!, selectedRightItem!!)
            if (isCorrect) {
                correctAnswersCount++
                // Удаляем правильные пары из адаптеров
                adapterLeft.removeItem(selectedLeftItem!!)
                adapterRight.removeItem(selectedRightItem!!)

                // Если все пары найдены – переходим к следующему вопросу
                if (correctAnswersCount >= totalPairs) {
                    goToNextQuestion()
                } else {
                    clearSelection()
                }
            } else {
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
        //parentActivity.goToNextQuestion() // Переход к следующему вопросу
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
