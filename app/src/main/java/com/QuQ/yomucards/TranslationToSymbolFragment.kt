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
import androidx.fragment.app.Fragment
import com.QuQ.yomucards.databinding.FragmentTranslationToSymbolBinding

class TranslationToSymbolFragment(
    var viewModel: TrainingViewModel
) : Fragment() {

    private lateinit var binding: FragmentTranslationToSymbolBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTranslationToSymbolBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val question = viewModel.questions.value?.get(parentActivity.currentQuestionIndex)
        if (question == null) {
            parentActivity.goToNextQuestion()
            return
        }

        binding.translationText.text = question.items.firstOrNull()?.second ?: ""
        binding.optionsRadioGroup.removeAllViews()

//        question.options.forEach { option ->
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
            val correctAnswer = question.items.firstOrNull()?.first ?: ""
            val isCorrect = selected?.tag == correctAnswer
            parentActivity.handleAnswer(isCorrect)
        }
    }

    private val parentActivity get() = activity as TrainingMyCardsActivity
}
