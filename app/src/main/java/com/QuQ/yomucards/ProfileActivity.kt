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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener

class ProfileActivity : ComponentActivity(){
    private lateinit var auth: FirebaseAuth

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val imageView: ImageView = findViewById(R.id.ivProfilePicture)
        imageView.setImageBitmap(User.imageProfile)
        val bitmap = intent.getParcelableExtra<Bitmap>("image")
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        }

        val textUsername: TextView = findViewById(R.id.tvUserName)
        textUsername.text = User.name

        val textId: TextView = findViewById(R.id.tvId)
        textId.text = "QID: " + User.id

        val textEmail: TextView = findViewById(R.id.tvEmail)
        textEmail.text = User.email

        auth = FirebaseAuth.getInstance()
        val signOutButton = findViewById<Button>(R.id.btnOutAccount)
        signOutButton.setOnClickListener {
            signOutAndNavigateToMain()
        }

        val editButton = findViewById<Button>(R.id.btnEdit)
        editButton.setOnClickListener{
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        val exitButton = findViewById<ImageButton>(R.id.btnExitProfile)
        exitButton.setOnClickListener{
            finish()
        }

        val textLessons: TextView = findViewById(R.id.tvLessonState)
        getLessonNumber { lessonNumber ->
            if (lessonNumber != null) {
                textLessons.text = "Пройдено уроков: " + lessonNumber
            } else {

            }
        }

        val textKanaState: TextView = findViewById(R.id.tvKanaState)
        getMyCardsCount { count ->
            if (count != null) {
                textKanaState.text = "Добавлено карточек: " + count
            } else {

            }
        }

        imageView.setImageBitmap(User.imageProfile)

    }

    fun getLessonNumber(callback: (Int?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
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
                    callback(null) // В случае ошибки возвращаем null
                }
            })
        } else {
            println("Ошибка: пользователь не авторизован")
            callback(null)
        }
    }

    fun getMyCardsCount(callback: (Int) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
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





    private fun signOutAndNavigateToMain() {
        // Выход из FirebaseAuth
        auth.signOut()

        // Выход из Google Sign-In
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.signOut().addOnCompleteListener {
            // Удаление сохраненных данных входа
            clearLoginInfo()

            // Переход на MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun clearLoginInfo() {
        val prefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()

        val imageView: ImageView = findViewById(R.id.ivProfilePicture)



        // Получаем Bitmap из Intent
        val bitmap = intent.getParcelableExtra<Bitmap>("image")
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            // Устанавливаем изображение в ImageView
            imageView.setImageBitmap(User.imageProfile)
        }, 3000) // 500 миллисекунд

        val textUsername: TextView = findViewById(R.id.tvUserName)
        textUsername.text = User.name

        val textId: TextView = findViewById(R.id.tvId)
        textId.text = "QID: " + User.id

        val textEmail: TextView = findViewById(R.id.tvEmail)
        textEmail.text = User.email

        val textLessons: TextView = findViewById(R.id.tvLessonState)
        getLessonNumber { lessonNumber ->
            if (lessonNumber != null) {
                textLessons.text = "Пройдено уроков: " + lessonNumber
            } else {

            }
        }

        val textKanaState: TextView = findViewById(R.id.tvKanaState)
        getMyCardsCount { count ->
            if (count != null) {
                textKanaState.text = "Добавлено карточек: " + count
            } else {

            }
        }

    }
}