package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.QuQ.yomucards.LessonState.MaxLesson
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FriendProfileActivity() : ComponentActivity(){
    private lateinit var auth: FirebaseAuth
    private lateinit var friend: Friend

    companion object {
        private const val EXTRA_FRIEND = "extra_friend"

        fun createIntent(context: Context, friend: Friend): Intent {
            return Intent(context, FriendProfileActivity::class.java).apply {
                putExtra(EXTRA_FRIEND, friend)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friend = intent.getParcelableExtra(EXTRA_FRIEND) ?: throw IllegalStateException("Friend is missing")
        setContentView(R.layout.activity_friend_profile)

        val imageView: ImageView = findViewById(R.id.ivFriendProfilePicture)
        Glide.with(imageView.context)
            .load(friend.avatarPath)
            .placeholder(R.drawable.ic_yo)
            .error(R.drawable.ic_yo)
            .into(imageView)

        val textUsername: TextView = findViewById(R.id.tvFriendUserName)
        textUsername.text = friend.username

        val textId: TextView = findViewById(R.id.tvFriendId)
        textId.text = "QID: " + friend.id

        val exitButton = findViewById<ImageButton>(R.id.btnExitFriendProfile)
        exitButton.setOnClickListener{
            finish()
        }

        val dellButton = findViewById<Button>(R.id.btnDellFriend)
        dellButton.setOnClickListener{
            val database = Firebase.database.reference
            val friendRequestPath = "Users/${User.id}/friends/${friend.id}"
            val friendRequestPath2 = "Users/${friend.id}/friends/${User.id}"
            database.child(friendRequestPath).removeValue()
            database.child(friendRequestPath2).removeValue()

            finish()
        }

        val textLessons: TextView = findViewById(R.id.tvFriendLessonState)
        getLessonNumber { lessonNumber ->
            if (lessonNumber != null) {
                textLessons.text = "Пройдено уроков: " + lessonNumber
            } else {
                textLessons.text = "Пройдено уроков: " + 0
            }
        }

        val textKanaState: TextView = findViewById(R.id.tvFriendKanaState)
        getMyCardsCount { count ->
            if (count != null) {
                textKanaState.text = "Добавлено карточек: " + count
            } else {
                textKanaState.text = "Добавлено карточек: " + 0
            }
        }

    }

    fun getLessonNumber(callback: (Int?) -> Unit) {
        val userId = friend.id
        if (userId != null) {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("Users/$userId/Stats_YomuCards/LessonNumber")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lessonNumber = snapshot.getValue(Int::class.java)
                    callback(lessonNumber) // Передаём значение в callback
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Ошибка чтения данных: ${error.message}")
                    callback(0) // В случае ошибки возвращаем null
                }
            })
        } else {
            println("Ошибка: пользователь не авторизован")
            callback(0)
        }
    }

    fun getMyCardsCount(callback: (Int) -> Unit) {
        val userId = friend.id
        if (userId == null) {
            println("Ошибка: пользователь не авторизован")
            callback(0)
            return
        }

        val database = FirebaseDatabase.getInstance()
        val paths = listOf(
            "Users/$userId/Stats_YomuCards/Cards/Hiragana",
            "Users/$userId/Stats_YomuCards/Cards/Katakana",
            "Users/$userId/Stats_YomuCards/Cards/Kanji"
        )

        var totalCount = 0
        var completedRequests = 0

        for (path in paths) {
            val ref = database.getReference(path)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val count = snapshot.childrenCount.toInt() // Количество записей
                        totalCount += count
                    }

                    completedRequests++
                    if (completedRequests == paths.size) {
                        callback(totalCount) // Возвращаем результат, когда все запросы завершены
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Ошибка чтения данных: ${error.message}")
                    completedRequests++
                    if (completedRequests == paths.size) {
                        callback(totalCount) // Если ошибка, всё равно возвращаем то, что есть
                    }
                }
            })
        }
    }
}