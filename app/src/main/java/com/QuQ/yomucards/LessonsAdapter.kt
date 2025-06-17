package com.QuQ.yomucards
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class LessonsAdapter(
    private var lessons: List<Lesson>,
    private val fragmentManager: androidx.fragment.app.FragmentManager
) : RecyclerView.Adapter<LessonsAdapter.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return if (someConditionBasedOn(position)) 0 else 1
    }

    fun someConditionBasedOn(position: Int): Boolean {
        val idx = position
        val mod = idx % 6
        return mod == 1 || mod == 5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == 0) R.layout.item_lesson_picture else R.layout.item_lesson
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val idx = position
        val mod = idx % 6
        if (!(mod == 1 || mod == 5)) {
            // Вычисляем количество пропущенных элементов до текущей позиции
            val x = idx / 6
            var totalSkipped = x * 2
            val y = idx % 6
            if(y > 1){
                totalSkipped += 1
            }
            val correctedPosition = idx - totalSkipped

            holder.bind(
                lessons[correctedPosition],
                correctedPosition < highlightedLessons,
                correctedPosition == highlightedLessons,
            )
        }
    }

    fun updateData(newLessons: List<Lesson>) {
        lessons = newLessons
        notifyDataSetChanged()
    }

    private var highlightedLessons = 0
    override fun getItemCount(): Int {
        // Повторяем ту же логику, что и в SnakeLayoutManager:
        // модификатор: true = картинка (пропуск), false = урок
        fun isPicPosition(adapterPos: Int): Boolean {
            val mod = adapterPos % 6
            return (mod == 1 || mod == 5)
        }

        var lessonsPlaced = 0    // сколько уроков мы уже «разместили»
        var totalPositions = 0   // сколько view (уроков+картинок) уже «заняли»

        while (lessonsPlaced < lessons.size) {
            if (!isPicPosition(totalPositions)) {
                // если это урок — считаем его
                lessonsPlaced++
            }
            // и в любом случае двигаемся к следующей позиции
            totalPositions++
        }

        return totalPositions
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(lesson: Lesson, isHighlighted: Boolean,isHighlightedNext: Boolean) {
            val button = itemView.findViewById<Button>(R.id.lessonButton)
            button.text = "${lesson.LessonID}"




            if (isHighlighted) {
                button.setTextColor(ColorStateList.valueOf(Color.parseColor("#04480B")) )
                button.setTypeface(null, Typeface.BOLD)
                button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#C1FFC1")) // Салатовый цвет
                button.setTextSize(30f)
                button.isEnabled = true
            }
            else if (isHighlightedNext) {
                button.setTextColor(Color.WHITE)
                button.setTypeface(null, Typeface.BOLD)
                button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B61E15")) // Красный цвет
                button.setTextSize(30f)
                button.isEnabled = true
            }
            else{
                button.setTextSize(30f)
                button.setTextColor(Color.WHITE)
                button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E5E5E5")) // Серый цвет
                button.setTypeface(null, Typeface.NORMAL)
                button.isEnabled = false
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

