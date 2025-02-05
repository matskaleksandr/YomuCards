package com.QuQ.yomucards

import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

data class Kana(val symbol: String, val pronunciation: String, val type: KanaType)

enum class KanaType {
    HIRAGANA,
    KATAKANA
}

class SearchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KanaAdapter
    private lateinit var databaseHelper: DatabaseHelper
    // Изменяемый список, который передаём адаптеру
    private val kanaList = mutableListOf<Kana>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)

        // Инициализация SearchView из разметки
        val searchView = view.findViewById<SearchView>(R.id.search_view)

        // Определяем количество колонок в зависимости от ориентации экрана
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 4 else 3
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

        // Передаём изменяемый список адаптеру
        adapter = KanaAdapter(kanaList)
        recyclerView.adapter = adapter

        // Загружаем первоначальные данные (без фильтра, или можно задать пустой запрос)
        loadDataWithQuery("")

        // Установка слушателя для отслеживания изменений текста
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Вызывается при изменении текста (добавление/удаление символов)
            override fun onQueryTextChange(newText: String?): Boolean {
                // Выполняем запрос с текущим текстом поиска
                loadDataWithQuery(newText ?: "")
                return false
            }

            // Вызывается при отправке запроса (например, нажатии кнопки "поиск" на клавиатуре)
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Можно обработать подтверждение запроса, если требуется
                loadDataWithQuery(query ?: "")
                return false
            }
        })

        return view
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

class KanaAdapter(private val kanaList: MutableList<Kana>) :
    RecyclerView.Adapter<KanaAdapter.KanaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KanaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kana_card, parent, false)
        return KanaViewHolder(view)
    }

    override fun onBindViewHolder(holder: KanaViewHolder, position: Int) {
        val kana = kanaList[position]
        holder.kanaSymbol.text = kana.symbol
        holder.kanaPronunciation.text = kana.pronunciation

        // Устанавливаем цвет текста в зависимости от типа
        when (kana.type) {
            KanaType.HIRAGANA -> holder.kanaSymbol.setTextColor(Color.BLUE) // Цвет для хираганы
            KanaType.KATAKANA -> holder.kanaSymbol.setTextColor(Color.GREEN) // Цвет для катаканы
        }
    }

    override fun getItemCount(): Int = kanaList.size

    class KanaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val kanaSymbol: TextView = itemView.findViewById(R.id.kana_symbol)
        val kanaPronunciation: TextView = itemView.findViewById(R.id.kana_pronunciation)
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
            db.rawQuery("SELECT kana, ENtranscription FROM Hiragana " +
                    "UNION " +
                    "SELECT kana, ENtranscription FROM Katakana",
                null)
        } else {
            val likeQueryKana = "%$query%"
            val likeQueryTranscription = "$query%"
            db.rawQuery(
                "SELECT Hiragana.kana, Hiragana.ENtranscription FROM Hiragana WHERE Hiragana.kana LIKE ? OR Hiragana.ENtranscription LIKE ? " +
                        "UNION " +
                        "SELECT Katakana.kana, Katakana.ENtranscription FROM Katakana WHERE Katakana.kana LIKE ? OR Katakana.ENtranscription LIKE ?",
                arrayOf("$query%", "$query%", "$query%", "$query%")
            )
        }

        cursor.use {
            while (it.moveToNext()) {
                val kana = it.getString(0)
                val ENtranscription = it.getString(1)
                // Определяем, является ли символ хираганой или катаканой
                val type = if (kana.all { it in 'ぁ'..'ん' }) KanaType.HIRAGANA else KanaType.KATAKANA
                kanaList.add(Kana(kana, ENtranscription, type))
            }
        }
        db.close()
        return kanaList
    }


    companion object {
        private const val DATABASE_VERSION = 1
    }
}
