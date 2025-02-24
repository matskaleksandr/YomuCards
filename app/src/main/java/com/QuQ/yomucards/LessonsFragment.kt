package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.FirebaseDatabase
import javax.sql.DataSource
import com.bumptech.glide.request.target.Target


class LessonsFragment : Fragment(R.layout.fragment_lessons) {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var drawerToggle: ActionBarDrawerToggle


    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        // Обновляем TextView с именем пользователя
        view?.findViewById<TextView>(R.id.textViewName)?.text = User.name ?: "Unknown"
        view?.findViewById<ImageView>(R.id.imageView)?.setImageBitmap(User.imageProfile)
    }


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

        val navigationView: NavigationView = view.findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    val intent = Intent(context, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_friends -> {
                    val intent = Intent(context, FriendsActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }


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
                        .load(it)
                        .placeholder(R.drawable.ic_yo)
                        .error(R.drawable.ic_yo)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                // Обработка ошибки загрузки (если необходимо)
                                return false  // Возвращаем false, чтобы Glide продолжил обработку ошибки
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                if (resource is BitmapDrawable) {
                                    User.imageProfile = resource.bitmap
                                }
                                return false  // Возвращаем false, чтобы Glide продолжил отображать изображение
                            }
                        })
                        .into(view.findViewById(R.id.imageView))
                }

            }
        }.addOnFailureListener {
            //Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        }



    }
}