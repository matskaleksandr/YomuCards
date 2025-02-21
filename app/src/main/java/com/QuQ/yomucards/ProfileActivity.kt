package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

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

        imageView.setImageBitmap(User.imageProfile)
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

    }
}