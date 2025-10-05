package com.QuQ.yomucards

import TopLearnerAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class TopLearner(
    val uid: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val lessonsCompleted: Int = 0
)

class TopLearnerActivity : AppCompatActivity() {

    private lateinit var btnExitMyCards: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TopLearnerAdapter
    private lateinit var tvUserPosition: TextView
    private lateinit var loadingOverlay: FrameLayout

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_learner)

        initViews()
        setupRecyclerView()
        setupExitButton()
        showLoading()
        loadTopLearners()
    }

    private fun initViews() {
        btnExitMyCards = findViewById(R.id.btnExitTopLearner)
        recyclerView = findViewById(R.id.topLearnerRecyclerView)
        tvUserPosition = findViewById(R.id.tvUserPosition)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    private fun setupRecyclerView() {
        adapter = TopLearnerAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupExitButton() {
        btnExitMyCards.setOnClickListener {
            finish()
        }
    }

    private fun showLoading() {
        loadingOverlay.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvUserPosition.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        tvUserPosition.visibility = View.VISIBLE
    }

    private fun loadTopLearners() {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val learners = mutableListOf<TopLearner>()
                val currentUserId = auth.currentUser?.uid
                var currentUser: TopLearner? = null
                var currentUserPosition = -1

                // Собираем всех пользователей с полем Stats_YomuCards
                for (userSnapshot in snapshot.children) {
                    val statsSnapshot = userSnapshot.child("Stats_YomuCards")

                    // Пропускаем пользователей без Stats_YomuCards
                    if (!statsSnapshot.exists()) continue

                    // Получаем количество пройденных уроков (0 если поля нет)
                    val lessonsCompleted = statsSnapshot.child("LessonNumber").getValue(Int::class.java) ?: 0

                    // Получаем данные пользователя
                    val uid = userSnapshot.key ?: continue
                    val name = userSnapshot.child("Username").getValue(String::class.java) ?: "Без имени"
                    val avatarUrl = userSnapshot.child("AvatarPath").getValue(String::class.java) ?: ""

                    val learner = TopLearner(uid, name, avatarUrl, lessonsCompleted)
                    learners.add(learner)

                    // Запоминаем текущего пользователя
                    if (uid == currentUserId) {
                        currentUser = learner
                    }
                }

                // Сортируем по убыванию количества уроков
                val sortedLearners = learners.sortedByDescending { it.lessonsCompleted }

                // Находим позицию текущего пользователя
                currentUserPosition = if (currentUserId != null) {
                    sortedLearners.indexOfFirst { it.uid == currentUserId } + 1
                } else {
                    -1
                }

                // Ограничиваем вывод до 100 пользователей, но добавляем текущего пользователя если он не в топ-100
                val limitedLearners = if (sortedLearners.size > 100) {
                    val top100 = sortedLearners.take(100)

                    // Если текущий пользователь не в топ-100, добавляем его в конец
                    if (currentUser != null && currentUserPosition > 100) {
                        top100 + currentUser
                    } else {
                        top100
                    }
                } else {
                    sortedLearners
                }

                // Устанавливаем данные в адаптер
                adapter.setData(limitedLearners)

                // Обновляем позицию пользователя
                updateUserPosition(currentUserPosition, currentUser?.lessonsCompleted ?: 0)

                // Скрываем индикатор загрузки
                hideLoading()

                // Логируем количество загруженных пользователей
                Log.d("TopLearnerActivity", "Loaded ${limitedLearners.size} users from ${learners.size} total")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TopLearnerActivity", "Error loading top learners: ${error.message}")
                tvUserPosition.text = "Ошибка загрузки рейтинга"
                hideLoading()
            }
        })
    }

    private fun updateUserPosition(position: Int, lessonsCompleted: Int) {
        val text = when {
            position > 100 -> "Ты на $position месте ($lessonsCompleted уроков)"
            position > 0 -> "Ты на $position месте ($lessonsCompleted уроков)"
            position == -1 -> "Войдите в аккаунт"
            else -> "Ты еще не в рейтинге"
        }
        tvUserPosition.text = text
    }
}