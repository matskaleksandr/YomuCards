package com.QuQ.yomucards

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class LessonsAdapter(
    private var lessons: List<Lesson>,
    private val fragmentManager: androidx.fragment.app.FragmentManager
) : RecyclerView.Adapter<LessonsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lessons[position], position < highlightedLessons, position == highlightedLessons)
    }

    fun updateData(newLessons: List<Lesson>) {
        lessons = newLessons
        notifyDataSetChanged()
    }

    private var highlightedLessons = 0
    override fun getItemCount() = lessons.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(lesson: Lesson, isHighlighted: Boolean,isHighlightedNext: Boolean) {
            val button = itemView.findViewById<Button>(R.id.lessonButton)
            button.text = "${lesson.LessonID}"


            if (isHighlighted) {
                button.setTextColor(Color.GREEN)
                button.setTypeface(null, Typeface.NORMAL)
                button.setTextSize(25f)
            }
            else if (isHighlightedNext) {
                button.setTextColor(Color.YELLOW)
                button.setTypeface(null, Typeface.BOLD)
                button.setTextSize(30f)
            }
            else{
                button.setTextSize(25f)
                button.setTextColor(Color.WHITE)
                button.setTypeface(null, Typeface.NORMAL)
            }
            button.setOnClickListener { openLesson(lesson) }
        }
    }

    fun updateHighlightedLessons(count: Int) {
        highlightedLessons = count
        notifyDataSetChanged()
    }

    private fun openLesson(lesson: Lesson) {
        val dialog = LessonInfoDialogFragment(lesson)
        dialog.show(fragmentManager, "LessonInfoDialog")
    }
}

