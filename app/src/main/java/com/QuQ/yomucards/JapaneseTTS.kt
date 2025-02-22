package com.QuQ.yomucards

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import java.util.*

class JapaneseTTS(context: Context) : OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Проверяем, поддерживается ли японский язык
            val result = tts?.setLanguage(Locale.JAPANESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Японский язык не поддерживается")
            } else {
                isInitialized = true
                Log.d("TTS", "TTS успешно инициализирован")
            }
        } else {
            Log.e("TTS", "Ошибка инициализации TTS")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TTS", "TTS не инициализирован")
        }
    }

    fun shutdown() {
        tts?.shutdown()
    }
}