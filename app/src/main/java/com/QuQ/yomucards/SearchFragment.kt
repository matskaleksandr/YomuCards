package com.QuQ.yomucards

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
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
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import java.io.File

data class Kana(
    val id: Int,
    val symbol: String,
    val pronunciation: String,
    val ruTranscription: String?,
    val ruTranslation: String?,
    val type: KanaType
)

enum class KanaType {
    HIRAGANA,
    KATAKANA,
    KANJI
}

class SearchFragment : Fragment(), OnCardRemovedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KanaAdapter
    private lateinit var databaseHelper: DatabaseHelper

    private lateinit var infoButton: ImageButton
    private lateinit var infoText: TextView

    // Изменяемый список, который передаём адаптеру
    private val kanaList = mutableListOf<Kana>()

    override fun onCardRemoved(kana: Kana) {
        // Обновляем список в SearchFragment
        val type = when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }
        adapter.addedCards[type]?.remove(kana.id.toString())
        adapter.notifyDataSetChanged() // Обновляем RecyclerView
    }
    override fun onCardAdded(kana: Kana) {
        val type = when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }
        adapter.addedCards[type]?.add(kana.id.toString())
        adapter.notifyItemChanged(adapter.findPositionById(kana.id)) // Обновляем конкретную позицию
    }


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
        adapter = KanaAdapter(
            kanaList,
            parentFragmentManager, // Используем parentFragmentManager для фрагментов
            {
                // Обработчик клика на элемент
            },
            this // Передаем OnCardRemovedListener
        )
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

        loadAddedCards()

        return view
    }

    private fun loadAddedCards() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val database = Firebase.database.reference
        val userId = currentUser.uid

        // Загружаем добавленные карточки для всех типов
        val types = listOf("Hiragana", "Katakana", "Kanji")
        val addedCards = mutableMapOf<String, MutableSet<String>>()

        types.forEach { type ->
            val path = "Users/$userId/Stats_YomuCards/Cards/$type"
            database.child(path).get().addOnSuccessListener { snapshot ->
                val ids = snapshot.children.map { it.key ?: "" }.toMutableSet()

                addedCards[type] = ids
                adapter.updateAddedCards(addedCards) // Обновляем адаптер
                adapter.notifyDataSetChanged()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
        loadAddedCards()
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
    private val fragmentManager: FragmentManager,
    private val onItemClick: () -> Unit,
    private val onCardRemovedListener: OnCardRemovedListener // Передаем слушатель
) : RecyclerView.Adapter<KanaAdapter.KanaViewHolder>() {

    fun findPositionById(id: Int): Int {
        return kanaList.indexOfFirst { it.id == id }
    }
    fun updateData(newData: List<Kana>) {
        kanaList.clear()
        kanaList.addAll(newData)
        notifyDataSetChanged()
    }
    // Храним добавленные карточки с учётом типа
    val addedCards = mutableMapOf<String, MutableSet<String>>()

    // Метод для обновления списка добавленных карточек
    fun updateAddedCards(newAddedCards: Map<String, MutableSet<String>>) {
        addedCards.clear()
        addedCards.putAll(newAddedCards)
        notifyDataSetChanged() // Обновляем все элементы
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KanaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kana_card, parent, false)
        return KanaViewHolder(view)
    }

    override fun onBindViewHolder(holder: KanaViewHolder, position: Int) {
        val kana = kanaList[position]
        holder.bind(kana)

        // Проверяем, добавлена ли карточка
        val cardType = when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }
        val isAdded = addedCards[cardType]?.contains(kana.id.toString()) ?: false
        holder.setAddedState(isAdded)

        //holder.itemView.animate().cancel()
        //holder.itemView.alpha = 0f
        //holder.itemView.animate().alpha(1f).setDuration(300).start()
    }

    override fun getItemCount(): Int = kanaList.size

    inner class KanaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val kanaSymbol: TextView = itemView.findViewById(R.id.kana_symbol)
        private val kanaPronunciation: TextView = itemView.findViewById(R.id.kana_pronunciation)
        private val greenCircle: ImageView = itemView.findViewById(R.id.green_circle)

        fun bind(kana: Kana) {
            kanaSymbol.text = kana.symbol
            kanaPronunciation.text = kana.pronunciation

            when (kana.type) {
                KanaType.HIRAGANA -> kanaSymbol.setTextColor(Color.BLUE)
                KanaType.KATAKANA -> kanaSymbol.setTextColor(Color.GREEN)
                KanaType.KANJI -> kanaSymbol.setTextColor(Color.BLACK)
            }

            val cardType = when (kana.type) {
                KanaType.HIRAGANA -> "Hiragana"
                KanaType.KATAKANA -> "Katakana"
                KanaType.KANJI -> "Kanji"
            }
            val isAdded = addedCards[cardType]?.contains(kana.id.toString()) ?: false
            setAddedState(isAdded)

            itemView.setOnClickListener {
                Log.d("KanaAdapter", "Элемент ${kana.symbol} нажат")
                showKanaInfoDialog(kana)
            }
        }

        fun setAddedState(isAdded: Boolean) {
            greenCircle.visibility = if (isAdded) View.VISIBLE else View.INVISIBLE
        }

        private fun showKanaInfoDialog(kana: Kana) {
            val dialog = KanaInfoDialogFragment(
                kana,
                this@KanaAdapter,
                this,
                {
                    val position = findPositionById(kana.id)
                    if (position != -1) notifyItemChanged(position)
                },
                onCardRemovedListener
            )
            dialog.show(fragmentManager, "KanaInfoDialog")
        }
    }
}


class DatabaseHelper(context: Context, fileName: String) : SQLiteOpenHelper(
    context, null, null, DATABASE_VERSION
) {
    private val databasePath: String = File(context.filesDir, fileName).absolutePath
    public var isDatabaseAvailable: Boolean = false

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
            val likeQuery = "$query%"
            db.rawQuery(
                """
            SELECT kana, ENtranscription, RUtranscription, NULL as RUtranslation, ID FROM Hiragana 
            WHERE kana LIKE ? OR ENtranscription LIKE ? OR RUtranscription LIKE ?
            UNION ALL
            SELECT kana, ENtranscription, RUtranscription, NULL as RUtranslation, ID FROM Katakana 
            WHERE kana LIKE ? OR ENtranscription LIKE ? OR RUtranscription LIKE ?
            UNION ALL
            SELECT kanj, ENtranscription, RUtranscription, RUtranslation, ID FROM Kanji 
            WHERE kanj LIKE ? OR ENtranscription LIKE ? OR RUtranscription LIKE ?
            """,
                arrayOf(
                    likeQuery, likeQuery, likeQuery,
                    likeQuery, likeQuery, likeQuery,
                    likeQuery, likeQuery, likeQuery
                )
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
                val id = it.getInt(4) // Получаем ID из курсора
                kanaList.add(Kana(id, kana, ENtranscription, RUtranscription, RUtranslation, type))
            }
        }
        db.close()
        return kanaList
    }


    companion object {
        private const val DATABASE_VERSION = 1
    }
}
