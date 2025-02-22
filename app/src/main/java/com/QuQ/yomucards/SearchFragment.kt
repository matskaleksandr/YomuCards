package com.QuQ.yomucards

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

data class Kana(
    val symbol: String,
    val pronunciation: String,
    val ruTranscription: String?, // Может быть null
    val ruTranslation: String?,   // Может быть null
    val type: KanaType
)

enum class KanaType {
    HIRAGANA,
    KATAKANA,
    KANJI
}

class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KanaAdapter
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var infoButton: ImageButton
    private lateinit var infoText: TextView

    // Изменяемый список, который передаём адаптеру
    private val kanaList = mutableListOf<Kana>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        infoButton = view.findViewById(R.id.info_button)
        infoText = view.findViewById(R.id.info_text)
        infoText.visibility = View.GONE

        infoButton.setOnClickListener {
            showInfoText()
        }

        // Устанавливаем обработчик клика по экрану для скрытия текста
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideInfoText()
            }
            false
        }

        recyclerView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val childView = recyclerView.findChildViewUnder(event.x, event.y)
                if (childView == null) {
                    hideInfoText()
                }
            }
            false
        }

        // Инициализация SearchView
        val searchView = view.findViewById<SearchView>(R.id.search_view)

        // Определяем количество колонок в зависимости от ориентации экрана
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 4 else 3
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

        // Передаём FragmentManager в адаптер
        adapter = KanaAdapter(kanaList, parentFragmentManager) {
            hideInfoText() // Вызываем hideInfoText при клике на элемент
        }
        recyclerView.adapter = adapter

        // Загружаем первоначальные данные
        loadDataWithQuery("")

        // Установка слушателя для отслеживания изменений текста
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                loadDataWithQuery(newText ?: "")
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                loadDataWithQuery(query ?: "")
                return false
            }
        })

        return view
    }

    private fun showInfoText() {
        // Показать текст с анимацией
        infoText.visibility = View.VISIBLE


        val text = """
        <font color="#0000FF">あ</font> – хирагана <br>
        <font color="#00FF00">カ</font> – катакана <br>
        <font color="#000000">漢</font> – кандзи
        """
        infoText.text = Html.fromHtml(text)



        // Настройка анимации для появления
        val fadeIn = ObjectAnimator.ofFloat(infoText, "alpha", 0f, 1f)
        fadeIn.duration = 500
        fadeIn.start()
    }

    private fun hideInfoText() {
        // Скрыть текст с анимацией
        val fadeOut = ObjectAnimator.ofFloat(infoText, "alpha", 1f, 0f)
        fadeOut.duration = 500

        // Запускаем анимацию
        fadeOut.start()

        // После окончания анимации скрываем текст с небольшой задержкой
        Handler(Looper.getMainLooper()).postDelayed({
            infoText.visibility = View.GONE
        }, fadeOut.duration)  // Задержка равна продолжительности анимации
    }




    override fun onResume() {
        super.onResume()
        // Загружаем данные без фильтра при возврате к фрагменту
        loadDataWithQuery("")
    }

    // Новый метод для загрузки данных по поисковому запросу
    private fun loadDataWithQuery(query: String) {
        val fileName = "yomucardsdb.db"
        val dbFile = File(requireContext().filesDir, fileName)

        if (dbFile.exists()) {
            Log.d("Database", "База данных найдена, выполняем запрос с фильтром: '$query'")
            // Инициализируем DatabaseHelper, если еще не проинициализирован
            if (!::databaseHelper.isInitialized) {
                databaseHelper = DatabaseHelper(requireContext(), fileName)
            }
            // Получаем новые данные по запросу
            val newData = databaseHelper.getKanaData(query)
            // Очищаем текущий список и добавляем новые данные
            kanaList.clear()
            kanaList.addAll(newData)
            Log.d("SearchFragment", "Загружено элементов: ${kanaList.size}")
            adapter.notifyDataSetChanged()
        } else {
            Log.e("Database", "Файл базы данных не найден!")
        }
    }
}

class KanaAdapter(
    private val kanaList: MutableList<Kana>,
    private val fragmentManager: FragmentManager, // Передаём FragmentManager
    private val onItemClick: () -> Unit
) : RecyclerView.Adapter<KanaAdapter.KanaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KanaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kana_card, parent, false)
        return KanaViewHolder(view)
    }

    override fun onBindViewHolder(holder: KanaViewHolder, position: Int) {
        val kana = kanaList[position]
        holder.kanaSymbol.text = kana.symbol
        holder.kanaPronunciation.text = kana.pronunciation
        holder.bind(kana)

        // Устанавливаем цвет текста в зависимости от типа
        when (kana.type) {
            KanaType.HIRAGANA -> holder.kanaSymbol.setTextColor(Color.BLUE)
            KanaType.KATAKANA -> holder.kanaSymbol.setTextColor(Color.GREEN)
            KanaType.KANJI -> holder.kanaSymbol.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int = kanaList.size

    inner class KanaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val kanaSymbol: TextView = itemView.findViewById(R.id.kana_symbol)
        val kanaPronunciation: TextView = itemView.findViewById(R.id.kana_pronunciation)

        fun bind(kana: Kana) {
            kanaSymbol.text = kana.symbol
            kanaPronunciation.text = kana.pronunciation

            itemView.setOnClickListener {
                onItemClick() // Вызов метода, переданного из фрагмента
                showKanaInfoDialog(kana) // Показываем диалог с информацией
            }
        }

        private fun showKanaInfoDialog(kana: Kana) {
            val dialog = KanaInfoDialogFragment(kana)
            dialog.show(fragmentManager, "KanaInfoDialog") // Используем переданный FragmentManager
        }
    }
}


class DatabaseHelper(context: Context, fileName: String) : SQLiteOpenHelper(
    context, null, null, DATABASE_VERSION
) {
    private val databasePath: String = File(context.filesDir, fileName).absolutePath

    override fun onCreate(db: SQLiteDatabase) {
        // База уже загружена, ничего не создаём
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Обновление структуры, если нужно
    }

    fun getDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    fun getVersionDB(): Int {
        val db = getDatabase()
        var version = -1 // Значение по умолчанию, если запрос не вернёт данных

        val cursor = db.rawQuery("SELECT Versions FROM versiondb", null)
        cursor.use {
            if (it.moveToFirst()) {
                version = it.getInt(0) // Получаем число из первой строки
                Log.d("DB", "Полученное значение: $version")
            }
        }
        db.close()
        return version
    }


    /**
     * Возвращает список Kana с учетом поискового запроса.
     * Если query пустой, возвращаются все записи.
     * Если query не пустой, выполняется поиск по полям kana и ENtranscription.
     */
    fun getKanaData(query: String?): List<Kana> {
        val kanaList = mutableListOf<Kana>()
        val db = getDatabase()

        val cursor = if (query.isNullOrEmpty()) {
            db.rawQuery(
                "SELECT kana, ENtranscription, RUtranscription, NULL as RUtranslation, ID, 1 as sort_order FROM Hiragana " +
                        "UNION ALL " +
                        "SELECT kana, ENtranscription, RUtranscription, NULL as RUtranslation, ID, 2 as sort_order FROM Katakana " +
                        "UNION ALL " +
                        "SELECT kanj, ENtranscription, RUtranscription, RUtranslation, ID, 3 as sort_order FROM Kanji " +
                        "ORDER BY sort_order, ID",
                null
            )
        } else {
            val likeQueryKana = "%$query%"
            val likeQueryTranscription = "$query%"
            db.rawQuery(
                "SELECT Hiragana.kana, Hiragana.ENtranscription, Hiragana.RUtranscription, NULL as RUtranslation FROM Hiragana WHERE Hiragana.kana LIKE ? OR Hiragana.ENtranscription LIKE ? OR Hiragana.RUtranscription LIKE ? " +
                        "UNION " +
                        "SELECT Katakana.kana, Katakana.ENtranscription, Katakana.RUtranscription, NULL as RUtranslation FROM Katakana WHERE Katakana.kana LIKE ? OR Katakana.ENtranscription LIKE ? OR Katakana.RUtranscription LIKE ? " +
                        "UNION " +
                        "SELECT Kanji.kanj, Kanji.ENtranscription, Kanji.RUtranscription, Kanji.RUtranslation FROM Kanji WHERE Kanji.kanj LIKE ? OR Kanji.ENtranscription LIKE ? OR Kanji.RUtranscription LIKE ?",
                arrayOf("$query%", "$query%", "$query%", "$query%", "$query%", "$query%", "$query%", "$query%", "$query%")
            )
        }

        cursor.use {
            while (it.moveToNext()) {
                val kana = it.getString(0)
                val ENtranscription = it.getString(1)
                val RUtranscription = it.getString(2) // Русская транскрипция
                val RUtranslation = it.getString(3)   // Русский перевод (может быть null)
                // Определяем тип символа
                val type = when {
                    kana.all { it in 'ぁ'..'ん' } -> KanaType.HIRAGANA
                    kana.all { it in 'ァ'..'ン' } -> KanaType.KATAKANA
                    else -> KanaType.KANJI
                }
                kanaList.add(Kana(kana, ENtranscription, RUtranscription, RUtranslation, type))
            }
        }
        db.close()
        return kanaList
    }


    companion object {
        private const val DATABASE_VERSION = 1
    }
}
