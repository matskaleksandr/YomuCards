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
import com.QuQ.yomucards.databinding.FragmentSoundToSymbolBinding

class SoundToSymbolFragment(
    private val viewModel: TrainingViewModel
) : Fragment() {

    private lateinit var binding: FragmentSoundToSymbolBinding
    private lateinit var tts: JapaneseTTS
    private var currentQuestionIndex: Int = 0

    companion object {
        private const val ARG_QUESTION_INDEX = "question_index"
        fun newInstance(questionIndex: Int, viewModel: TrainingViewModel): SoundToSymbolFragment {
            val fragment = SoundToSymbolFragment(viewModel)
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
        binding = FragmentSoundToSymbolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tts = JapaneseTTS(requireContext())

        val question = viewModel.questions.value?.getOrNull(currentQuestionIndex)
        if (question == null) {
            (activity as? TrainingMyCardsActivity)?.goToNextQuestion()
            return
        }

        // При нажатии на кнопку воспроизведения звучит аудио (используя TTS)
        binding.playButton.setOnClickListener {
            tts.speak(question.items.firstOrNull()?.first ?: "")
        }

        binding.optionsRadioGroup.removeAllViews()
        // Перемешиваем варианты ответов и создаем RadioButton для каждого варианта
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
            if (selected == null) {
                // Если вариант не выбран – можно показать сообщение об ошибке
                return@setOnClickListener
            }
            // Правильный ответ – второй элемент пары (т.е. символ)
            val correctAnswer = question.items.firstOrNull()?.second ?: ""
            val isCorrect = selected.tag == correctAnswer
            if (!isCorrect) {
                return@setOnClickListener
            }
            (activity as? TrainingMyCardsActivity)?.handleAnswer(isCorrect)
        }
    }
}
