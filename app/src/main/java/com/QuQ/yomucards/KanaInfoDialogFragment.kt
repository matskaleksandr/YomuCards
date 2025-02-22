package com.QuQ.yomucards

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class KanaInfoDialogFragment(private val kana: Kana) : DialogFragment() {

    private lateinit var japaneseTTS: JapaneseTTS

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_kana_info, null)
        japaneseTTS = JapaneseTTS(requireContext())

        val btnPronunciation = view.findViewById<Button>(R.id.btnPronunciation)
        btnPronunciation.setOnClickListener {
            val text = kana.symbol // Текст для озвучивания
            japaneseTTS.speak(text)
        }


        view.background = ContextCompat.getDrawable(context, R.drawable.dialog_background)

        val kanaSymbol = view.findViewById<TextView>(R.id.dialog_kana_symbol)
        val kanaPronunciation = view.findViewById<TextView>(R.id.dialog_kana_pronunciation)
        val kanaRuTranslation = view.findViewById<TextView>(R.id.dialog_kana_ru_translation)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)
        btnClose.setOnClickListener {
            dismiss() // Закрыть диалог
        }

        kanaSymbol.text = kana.symbol
        kanaPronunciation.text = "Произношение: ${kana.pronunciation} / ${kana.ruTranscription}"
        kanaRuTranslation.text = "Перевод: ${kana.ruTranslation ?: "Нету"}"

        return AlertDialog.Builder(context, R.style.CustomDialogTheme) // Применяем стиль
            .setView(view)
            .create()
    }
}