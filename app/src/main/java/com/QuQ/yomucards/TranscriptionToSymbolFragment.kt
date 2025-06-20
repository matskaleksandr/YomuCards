package com.QuQ.yomucards

import android.R
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.QuQ.yomucards.databinding.FragmentTranscriptionToSymbolBinding

class TranscriptionToSymbolFragment(
    private val viewModel: TrainingViewModel
) : Fragment() {

    private lateinit var binding: FragmentTranscriptionToSymbolBinding
    private var currentQuestionIndex: Int = 0

    companion object {
        private const val ARG_QUESTION_INDEX = "question_index"
        fun newInstance(questionIndex: Int, viewModel: TrainingViewModel): TranscriptionToSymbolFragment {
            val fragment = TranscriptionToSymbolFragment(viewModel)
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranscriptionToSymbolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val question = viewModel.questions.value?.getOrNull(currentQuestionIndex)
        if (question == null) {
            (activity as? TrainingMyCardsActivity)?.goToNextQuestion()
            return
        }

        binding.transcriptionText.text = question.items.firstOrNull()?.first ?: ""
        binding.optionsRadioGroup.removeAllViews()

//        question.options.shuffled().forEach { option ->
//            val radioButton = RadioButton(requireContext()).apply {
//                text = option
//                tag = option
//                setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
//            }
//            binding.optionsRadioGroup.addView(radioButton)
//        }
        question.options.shuffled().forEachIndexed { index, option ->
            val marginValue = 190
            val radioButton = RadioButton(requireContext()).apply {
                text = option
                tag = option
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
                buttonTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(R.attr.state_checked),
                        intArrayOf(-R.attr.state_checked)
                    ),
                    intArrayOf(
                        0xFFB00020.toInt(), // фиолетовый для выбранного
                        0xFFAAAAAA.toInt()  // серый по умолчанию
                    )
                )
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Добавляем margin справа для всех, кроме последнего
                    rightMargin = if (index != question.options.size - 1) marginValue else 0
                }
            }
            binding.optionsRadioGroup.addView(radioButton)
        }


        binding.submitButton.setOnClickListener {
            val selected = binding.optionsRadioGroup.findViewById<RadioButton>(
                binding.optionsRadioGroup.checkedRadioButtonId
            )
            if (selected == null) {
                Toast.makeText(requireContext(), "Выберите вариант ответа!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val correctAnswer = question.items.firstOrNull()?.second ?: ""
            val isCorrect = selected.tag == correctAnswer
            if (!isCorrect) {
                return@setOnClickListener
            }
            (activity as? TrainingMyCardsActivity)?.handleAnswer(isCorrect)
        }
    }
}
