package com.QuQ.yomucards

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class TrainingPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val viewModel: TrainingViewModel
) : FragmentStateAdapter(fragmentActivity) {

    // Переопределяем getItemId, чтобы адаптер мог корректно обновлять фрагменты
    override fun getItemId(position: Int): Long {
        return viewModel.questions.value?.get(position)?.hashCode()?.toLong() ?: super.getItemId(position)
    }

    // Переопределяем containsItem, чтобы адаптер мог корректно обновлять фрагменты
    override fun containsItem(itemId: Long): Boolean {
        return viewModel.questions.value?.any { it.hashCode().toLong() == itemId } ?: false
    }

    override fun getItemCount(): Int = 20

    override fun createFragment(position: Int): Fragment {
        val question = viewModel.questions.value?.get(position)
        return when (question?.type) {
            QuestionType.SYMBOL_TO_TRANSCRIPTION -> SymbolToTranscriptionFragment(viewModel)
            QuestionType.TRANSCRIPTION_TO_SYMBOL -> TranscriptionToSymbolFragment(viewModel)
            QuestionType.SYMBOL_TO_TRANSLATION -> SymbolToTranslationFragment(viewModel)
            QuestionType.MATCHING_TRANSCRIPTION -> MatchingFragment(viewModel)
            QuestionType.MATCHING_SYMBOL -> MatchingFragment(viewModel)
            QuestionType.SOUND_TO_SYMBOL -> SoundToSymbolFragment(viewModel)
            else -> throw IllegalArgumentException("Unknown question type")
        }
    }

    // Метод для обновления адаптера
    @SuppressLint("NotifyDataSetChanged")
    public fun updateQuestions() {
        notifyDataSetChanged()
    }
}
