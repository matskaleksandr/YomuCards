package com.QuQ.yomucards

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TrainingPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val viewModel: TrainingViewModel
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 20

    override fun createFragment(position: Int): Fragment {
        return when (viewModel.questions.value?.get(position)?.type) {
            QuestionType.SYMBOL_TO_TRANSCRIPTION -> SymbolToTranscriptionFragment(viewModel)
            QuestionType.TRANSCRIPTION_TO_SYMBOL -> TranscriptionToSymbolFragment(viewModel)
            //QuestionType.TRANSLATION_TO_SYMBOL -> TranslationToSymbolFragment(viewModel)
            QuestionType.SYMBOL_TO_TRANSLATION -> SymbolToTranslationFragment(viewModel)
            QuestionType.MATCHING_TRANSCRIPTION -> MatchingFragment(viewModel)
            QuestionType.MATCHING_SYMBOL -> MatchingFragment(viewModel)
            //QuestionType.MATCHING_TRANSLATION -> MatchingFragment(viewModel)
            QuestionType.SOUND_TO_SYMBOL -> SoundToSymbolFragment(viewModel)
            else -> throw IllegalArgumentException("Unknown question type")
        }
    }
}
