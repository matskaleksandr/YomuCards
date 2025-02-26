package com.QuQ.yomucards

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.QuQ.yomucards.databinding.FragmentSoundToSymbolBinding

class SoundToSymbolFragment(
    var viewModel: TrainingViewModel,
) : Fragment() {

    private lateinit var binding: FragmentSoundToSymbolBinding
    private lateinit var tts: JapaneseTTS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSoundToSymbolBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tts = JapaneseTTS(requireContext())

        val question = viewModel.questions.value?.get(parentActivity.currentQuestionIndex)
        if (question == null) {
            parentActivity.goToNextQuestion()
            return
        }

        binding.playButton.setOnClickListener {
            tts.speak(question.items.firstOrNull()?.first ?: "")
        }
        
        binding.optionsRadioGroup.removeAllViews()

        // Перемешиваем варианты ответов
        question.options.shuffled().forEach { option ->
            val radioButton = RadioButton(requireContext()).apply {
                text = option
                tag = option
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            }
            binding.optionsRadioGroup.addView(radioButton)
        }


        binding.submitButton.setOnClickListener {
            val selected = binding.optionsRadioGroup.findViewById<RadioButton>(
                binding.optionsRadioGroup.checkedRadioButtonId
            )
            if (selected == null) {
                // Пользователь не выбрал вариант ответа
                // Можно показать сообщение об ошибке
                return@setOnClickListener
            }
            // Проверяем, совпадает ли выбранный ответ с правильным (первый элемент items)
            val correctAnswer = question.items.firstOrNull()?.second ?: ""
            val isCorrect = selected.tag == correctAnswer
            if(!isCorrect){
                return@setOnClickListener
            }
            parentActivity.handleAnswer(isCorrect)
        }

    }

    private val parentActivity get() = activity as TrainingMyCardsActivity
}
