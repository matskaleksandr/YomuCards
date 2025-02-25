package com.QuQ.yomucards

import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.database.database



class MyCardsActivity : AppCompatActivity(), OnCardRemovedListener {
    private lateinit var adapter: KanaAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private val addedCards = mutableMapOf<String, MutableSet<String>>()
    private lateinit var recycler_view_MyCards: RecyclerView
    private lateinit var btnExitMyCards: ImageButton

    private lateinit var searchView: SearchView
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_cards)

        recycler_view_MyCards = findViewById(R.id.recycler_view_MyCards)
        btnExitMyCards = findViewById(R.id.btnExitMyCards)

        searchView = findViewById(R.id.search_view_MyCards)
        setupSearchView()

        setupRecyclerView()
        loadAddedCards()
        setupExitButton()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText?.trim() ?: ""
                loadFilteredData()
                return true
            }
        })
    }



    override fun onCardRemoved(kana: Kana) {
        // Удаляем карточку из списка
        val type = when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }
        addedCards[type]?.remove(kana.id.toString())
        loadFilteredData() // Обновляем список
    }
    override fun onCardAdded(kana: Kana) {

    }

    private fun setupRecyclerView() {
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 4 else 3
        recycler_view_MyCards.layoutManager = GridLayoutManager(this, spanCount)

        adapter = KanaAdapter(
            mutableListOf(),
            supportFragmentManager, // Передаем supportFragmentManager
            {
                // Обработчик клика на элемент
            },
            this // Передаем OnCardRemovedListener
        )
        recycler_view_MyCards.adapter = adapter
    }

    private fun loadAddedCards() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val database = Firebase.database.reference
        val userId = currentUser.uid

        listOf("Hiragana", "Katakana", "Kanji").forEach { type ->
            database.child("Users/$userId/Stats_YomuCards/Cards/$type")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val ids = snapshot.children.map { it.key ?: "" }.toMutableSet()
                        addedCards[type] = ids
                        loadFilteredData()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MyCardsActivity, "Ошибка загрузки: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun loadFilteredData() {
        databaseHelper = DatabaseHelper(this, "yomucardsdb.db")
        val allData = databaseHelper.getKanaData(currentQuery) // Фильтрация через БД
            .filter { kana ->
                val type = when (kana.type) {
                    KanaType.HIRAGANA -> "Hiragana"
                    KanaType.KATAKANA -> "Katakana"
                    KanaType.KANJI -> "Kanji"
                }
                addedCards[type]?.contains(kana.id.toString()) == true
            }

        adapter.updateData(allData)
    }

    private fun setupExitButton() {
        btnExitMyCards.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAddedCards()
    }
}