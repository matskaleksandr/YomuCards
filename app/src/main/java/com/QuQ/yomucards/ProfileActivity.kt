package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity

class ProfileActivity : ComponentActivity(){
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val imageView: ImageView = findViewById(R.id.ivProfilePicture)
        imageView.setImageBitmap(User.imageProfile)

        val textUsername: TextView = findViewById(R.id.tvUserName)
        textUsername.text = User.name

        val textId: TextView = findViewById(R.id.tvId)
        textId.text = "QID: " + User.id

        val textEmail: TextView = findViewById(R.id.tvEmail)
        textEmail.text = User.email

    }


}