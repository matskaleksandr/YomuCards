package com.QuQ.yomucards

import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

data class Kana(val symbol: String, val pronunciation: String)

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

        // Определяем количество колонок в зависимости от ориентации экрана
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 3
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)

        // Передаём изменяемый список адаптеру
        adapter = KanaAdapter(kanaList)
        recyclerView.adapter = adapter

        loadDataIfDatabaseExists() // Загружаем данные

        return view
    }

    override fun onResume() {
        super.onResume()
        loadDataIfDatabaseExists()
    }

    private fun loadDataIfDatabaseExists() {
        val fileName = "yomucardsdb.db"
        val dbFile = File(requireContext().filesDir, fileName)

        if (dbFile.exists()) {
            Log.d("Database", "База данных найдена, загружаем данные.")
            databaseHelper = DatabaseHelper(requireContext(), fileName)
            // Очищаем и заполняем один и тот же изменяемый список
            kanaList.clear()
            kanaList.addAll(databaseHelper.getKanaData())
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

    fun getKanaData(): List<Kana> {
        val kanaList = mutableListOf<Kana>()
        val db = getDatabase()
        val cursor = db.rawQuery("SELECT kana, ENtranscription FROM Hiragana", null)

        cursor.use {
            while (it.moveToNext()) {
                val kana = it.getString(0)
                val ENtranscription = it.getString(1)
                kanaList.add(Kana(kana, ENtranscription))
            }
        }
        db.close()
        return kanaList
    }

    companion object {
        private const val DATABASE_VERSION = 1
    }
}
