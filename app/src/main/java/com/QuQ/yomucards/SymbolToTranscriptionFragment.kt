package com.QuQ.yomucards

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.QuQ.yomucards.databinding.FragmentSymbolToTranscriptionBinding

class SymbolToTranscriptionFragment(
    private val viewModel: TrainingViewModel
) : Fragment() {

    private var _binding: FragmentSymbolToTranscriptionBinding? = null
    private val binding get() = _binding!!
    private var currentQuestionIndex: Int = 0

    companion object {
        private const val ARG_QUESTION_INDEX = "question_index"
        fun newInstance(questionIndex: Int, viewModel: TrainingViewModel): SymbolToTranscriptionFragment {
            val fragment = SymbolToTranscriptionFragment(viewModel)
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
        _binding = FragmentSymbolToTranscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        val question = viewModel.questions.value?.getOrNull(currentQuestionIndex) ?: run {
            (activity as? TrainingMyCardsActivity)?.goToNextQuestion()
            return
        }
        binding.symbolText.text = question.items.firstOrNull()?.first ?: ""
        binding.optionsRadioGroup.removeAllViews()

        question.options.shuffled().forEach { option ->
            val radioButton = RadioButton(requireContext()).apply {
                text = option
                tag = option
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            }
            binding.optionsRadioGroup.addView(radioButton)
        }
    }

    private fun setupListeners() {
        binding.submitButton.setOnClickListener {
            val selected = binding.optionsRadioGroup.findViewById<RadioButton>(
                binding.optionsRadioGroup.checkedRadioButtonId
            )
            if (selected == null) {
                Toast.makeText(requireContext(), "Выберите вариант ответа!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val correctAnswer = viewModel.questions.value
                ?.getOrNull(currentQuestionIndex)
                ?.items
                ?.firstOrNull()
                ?.second ?: ""
            val isCorrect = selected.tag == correctAnswer
            (activity as? TrainingMyCardsActivity)?.handleAnswer(isCorrect)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
