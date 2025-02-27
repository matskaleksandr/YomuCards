package com.QuQ.yomucards

enum class QuestionType {
    SYMBOL_TO_TRANSCRIPTION,
    TRANSCRIPTION_TO_SYMBOL,
    //TRANSLATION_TO_SYMBOL,
    SYMBOL_TO_TRANSLATION,
    MATCHING_TRANSCRIPTION,
    MATCHING_SYMBOL,
    //MATCHING_TRANSLATION,
    SOUND_TO_SYMBOL
}
data class TrainingQuestion(
    val type: QuestionType,
    val items: List<Pair<String?, String>> = emptyList(),
    val options: List<String> = emptyList(),
    var isAnsweredCorrectly: Boolean = false
)