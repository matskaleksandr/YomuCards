package com.QuQ.yomucards

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase

class LessonsFragment : Fragment(R.layout.fragment_lessons) {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawerLayout = view.findViewById(R.id.drawer_layout)
        toolbar = view.findViewById(R.id.toolbar)

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        // Отключаем заголовок приложения в Toolbar
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        drawerToggle = ActionBarDrawerToggle(
            requireActivity(),
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()



        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(User.id)
        databaseRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val avatarUrl = snapshot.child("AvatarPath").getValue(String::class.java)
                val username = snapshot.child("Username").getValue(String::class.java)

                // Устанавливаем имя пользователя
                view.findViewById<TextView>(R.id.textViewName).text = username

                // Загружаем изображение с URL в ImageView с помощью Glide
                avatarUrl?.let {
                    Glide.with(this)
                        .load(it) // Загружаем URL-изображение
                        .placeholder(R.drawable.ic_yo) // Изображение-заглушка, если загрузка идёт
                        .error(R.drawable.ic_yo) // Если не удалось загрузить
                        .into(view.findViewById(R.id.imageView)) // Указываем, куда загрузить
                }
            }
        }.addOnFailureListener {
            //Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        }

    }
}