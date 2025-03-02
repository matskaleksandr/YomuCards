package com.QuQ.yomucards

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SnakeLayoutManager : RecyclerView.LayoutManager() {

    private val verticalSpacing = 200  // Отступ между строками
    private var totalHeight = 0  // Полная высота списка
    private var verticalOffset = 0  // Текущий сдвиг при скролле

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachAndScrapAttachedViews(recycler)

        if (itemCount == 0) {
            totalHeight = 0
            verticalOffset = 0
            return
        }

        val parentWidth = width
        val columnWidth = parentWidth / 3  // Размер колонок
        val centerX = parentWidth / 2 - columnWidth / 2  // Центр первой колонки
        val indent = columnWidth  // Шаг вправо или влево

        var offsetX: Int
        var offsetY = 0  // Начало списка

        totalHeight = 0  // Сбрасываем перед пересчетом

        for (i in 0 until itemCount) {
            val view = recycler.getViewForPosition(i)
            addView(view)

            // Измеряем View перед размещением
            measureChildWithMargins(view, 0, 0)
            val itemWidth = getDecoratedMeasuredWidth(view)
            val itemHeight = getDecoratedMeasuredHeight(view)

            // Центрируем элемент внутри колонки
            val columnCenterOffset = (columnWidth - itemWidth) / 2

            // Вычисляем координаты змейкой
            offsetX = when (i % 4) {
                0 -> centerX + columnCenterOffset  // 1, 3, 5 - по центру
                1 -> centerX + indent + columnCenterOffset  // 2 - вправо
                2 -> centerX + columnCenterOffset  // 3 - по центру
                3 -> centerX - indent + columnCenterOffset  // 4 - влево
                else -> centerX + columnCenterOffset
            }

            // ❌ Убрали проверку на видимость! Теперь все элементы размещаются
            layoutDecorated(view, offsetX, offsetY - verticalOffset, offsetX + itemWidth, offsetY - verticalOffset + itemHeight)

            offsetY += itemHeight + verticalSpacing  // Добавляем отступ вниз
        }

        totalHeight = offsetY  // Теперь это точно правильная высота списка
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        val maxScroll = (totalHeight - height).coerceAtLeast(0)  // Учёт полной высоты списка
        val newOffset = (verticalOffset + dy).coerceIn(0, maxScroll)

        val scrollAmount = newOffset - verticalOffset
        verticalOffset = newOffset

        offsetChildrenVertical(-scrollAmount)  // Смещаем элементы
        return scrollAmount
    }
}
