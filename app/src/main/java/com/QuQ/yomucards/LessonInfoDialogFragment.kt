package com.QuQ.yomucards

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

class LessonInfoDialogFragment(
    private val lesson: Lesson
): DialogFragment() {
    private lateinit var textinfo: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_lesson_info, null)
        val lessonInfo = lesson.Items?.joinToString(separator = "\n") { "${it.symbol} - ${it.pronunciation}" }

        textinfo = view.findViewById(R.id.dialog_lesson_info_text)
        textinfo.text = lessonInfo

        view.background = ContextCompat.getDrawable(context, R.drawable.dialog_background)

        val btnClose = view.findViewById<Button>(R.id.btnCloseInfo)
        btnClose.setOnClickListener {
            dismiss()
        }


        val btnStart = view.findViewById<Button>(R.id.btnStartLesson)
        btnStart.setOnClickListener {
            LessonState.id = 1
            LessonState.kana = lesson.Items
            LessonState.LessonNumber = lesson.LessonID
            val intent = Intent(context, TrainingMyCardsActivity::class.java)
            startActivity(intent)
            dismiss()
        }



        return AlertDialog.Builder(context, R.style.CustomDialogTheme) // Применяем стиль
            .setView(view)
            .create()

    }
}