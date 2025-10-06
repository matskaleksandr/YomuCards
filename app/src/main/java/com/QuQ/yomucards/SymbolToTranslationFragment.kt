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
import com.QuQ.yomucards.databinding.FragmentSymbolToTranslationBinding

class SymbolToTranslationFragment(
    var viewModel: TrainingViewModel
) : Fragment() {

    private lateinit var binding: FragmentSymbolToTranslationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSymbolToTranslationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val question = viewModel.questions.value?.get(parentActivity.currentQuestionIndex)
        if (question == null) {
            parentActivity.goToNextQuestion()
            return
        }

        binding.symbolText.text = question.items.firstOrNull()?.first ?: ""
        binding.optionsRadioGroup.removeAllViews()

        // Перемешиваем варианты ответов
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                buttonTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        0xFFB00020.toInt(), // цвет выбранного
                        0xFFAAAAAA.toInt()  // серый по умолчанию
                    )
                )
                gravity = Gravity.START or Gravity.CENTER_VERTICAL // текст слева, кружок слева, выравнивание по вертикали
                layoutParams = RadioGroup.LayoutParams(
                    0, // ширина 0dp для веса
                    RadioGroup.LayoutParams.WRAP_CONTENT,
                    1f // равная доля для каждого RadioButton
                )
                setPadding(0, 0, 0, 0) // убираем лишние паддинги
            }
            binding.optionsRadioGroup.addView(radioButton)
        }



        binding.submitButton.setOnClickListener {
            val selected = binding.optionsRadioGroup.findViewById<RadioButton>(
                binding.optionsRadioGroup.checkedRadioButtonId
            )
            val correctAnswer = question.items.firstOrNull()?.second ?: ""
            val isCorrect = selected?.tag == correctAnswer
            parentActivity.handleAnswer(isCorrect)
        }
    }

    private val parentActivity get() = activity as TrainingMyCardsActivity
}
