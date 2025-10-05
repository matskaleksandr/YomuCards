package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.QuQ.yomucards.LessonState.MaxLesson
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.FirebaseDatabase
import javax.sql.DataSource
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class LessonsFragment : Fragment(R.layout.fragment_lessons) {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var adapter: LessonsAdapter
    private lateinit var recyclerView: RecyclerView
    private var menuItemsContainer: LinearLayout? = null // Делаем nullable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.lessonsRecyclerView)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        toolbar = view.findViewById(R.id.toolbar)




        val homeActivity = requireActivity() as HomeActivity
        databaseHelper = homeActivity.databaseHelper
        recyclerView = view.findViewById(R.id.lessonsRecyclerView)
        setupRecyclerView()
        checkDatabaseState()

        getLessonNumber { lessonNumber ->
            if (lessonNumber != null) {
                adapter.updateHighlightedLessons(lessonNumber)
                MaxLesson = lessonNumber
            } else {
                MaxLesson = 0
            }
        }

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
                menuItemsContainer = view.findViewById(R.id.menu_items_container)
                inflateMenuToContainer()
                val navView = view.findViewById<NavigationView>(R.id.nav_view)
                val header = navView.getHeaderView(0)

                // Устанавливаем высоту header как высоту NavigationView
                header.layoutParams.height = navView.height - 90
                header.requestLayout()

                val btnTelegram = view.findViewById<ImageButton>(R.id.btnTelegram)
                btnTelegram.setOnClickListener {
                    val url = "https://t.me/quq_basement"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }
                val btnDiscord = view.findViewById<ImageButton>(R.id.btnDiscord)
                btnDiscord.setOnClickListener {
                    val url = "https://discord.gg/WrGFY3qk3V"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }
                val btnVK = view.findViewById<ImageButton>(R.id.btnVk)
                btnVK.setOnClickListener {
                    val url = "https://vk.com/q_u_q"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }
                val btnYT = view.findViewById<ImageButton>(R.id.btnYoutube)
                btnYT.setOnClickListener {
                    val url = "https://www.youtube.com/@q_u_q"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }

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
                                return false
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
                                return false
                            }
                        })
                        .into(view.findViewById(R.id.imageView))


                }
            }
        }.addOnFailureListener {
            // Обработка ошибки
        }

    }

    @SuppressLint("SetTextI18n", "RestrictedApi")
    override fun onStart() {
        super.onStart()

        // Инициализация в onStart()




        getLessonNumber { lessonNumber ->
            if (lessonNumber != null) {
                adapter.updateHighlightedLessons(lessonNumber)
                MaxLesson = lessonNumber
            } else {
                MaxLesson = 0
            }
        }


    }

    override fun onResume() {
        super.onResume()
        getLessonNumber { lessonNumber ->
            if (lessonNumber != null) {
                adapter.updateHighlightedLessons(lessonNumber)
                MaxLesson = lessonNumber
            } else {
                MaxLesson = 0
            }
        }

        inflateMenuToContainer()
    }

    @SuppressLint("RestrictedApi")
    private fun inflateMenuToContainer() {
        // Используем безопасный вызов для nullable переменной
        menuItemsContainer?.let { container ->
            Log.d("LessonsFragment", "inflateMenuToContainer called")
            Log.d("LessonsFragment", "menuItemsContainer child count before: ${container.childCount}")

            // Очищаем контейнер перед добавлением новых элементов
            container.removeAllViews()
            Log.d("LessonsFragment", "menuItemsContainer child count after clear: ${container.childCount}")

            // Массивы с данными меню
            val menuIds = intArrayOf(
                R.id.nav_profile, R.id.nav_friends, R.id.nav_top_learner,
                R.id.nav_note, R.id.nav_cards, R.id.nav_trenning, R.id.nav_settings
            )
            val menuTitles = arrayOf(
                "Профиль", "Друзья", "Топ пользователей",
                "Мои записи", "Мои карточки", "Повторение карточек", "Настройки"
            )
            val menuIcons = intArrayOf(
                R.drawable.profile_button, R.drawable.friends_button_1, R.drawable.top_button,
                R.drawable.note_button_1, R.drawable.mycards_button, R.drawable.mycardstraining_button, R.drawable.settings_button
            )

            Log.d("LessonsFragment", "Creating ${menuIds.size} menu items")

            for (i in menuIds.indices) {
                Log.d("LessonsFragment", "Creating menu item $i: ${menuTitles[i]}")

                val menuItemView = createMenuItemView(menuIds[i], menuTitles[i], menuIcons[i])
                Log.d("LessonsFragment", "Menu item view created: ${menuItemView != null}")

                container.addView(menuItemView)
                Log.d("LessonsFragment", "Menu item $i added to container")

                // Добавляем разделитель
//                if (i < menuIds.size - 1) {
//                    val divider = View(requireContext()).apply {
//                        layoutParams = LinearLayout.LayoutParams(
//                            ViewGroup.LayoutParams.MATCH_PARENT, 1
//                        ).apply {
//                            setMargins(16, 8, 16, 8)
//                        }
//                        setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
//                    }
//                    container.addView(divider)
//                    Log.d("LessonsFragment", "Divider $i added")
//                }
            }

            Log.d("LessonsFragment", "Menu inflated successfully with ${menuIds.size} items")
            Log.d("LessonsFragment", "menuItemsContainer final child count: ${container.childCount}")
        } ?: run {
            Log.e("LessonsFragment", "menuItemsContainer is null in inflateMenuToContainer")
        }
    }

    private fun createMenuItemView(menuId: Int, title: String, iconRes: Int): View {
        Log.d("LessonsFragment", "createMenuItemView: $title, icon: $iconRes")

        val menuItemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.custom_menu_item, menuItemsContainer, false)

        val icon = menuItemView.findViewById<ImageView>(R.id.menu_item_icon)
        val titleView = menuItemView.findViewById<TextView>(R.id.menu_item_title)

        Log.d("LessonsFragment", "Icon view found: ${icon != null}")
        Log.d("LessonsFragment", "Title view found: ${titleView != null}")

        // Проверяем существование ресурсов
        try {
            icon.setImageResource(iconRes)
            Log.d("LessonsFragment", "Icon resource set successfully")
        } catch (e: Exception) {
            Log.e("LessonsFragment", "Error setting icon resource: $iconRes", e)
        }

        titleView.text = title
        Log.d("LessonsFragment", "Title set: $title")

        menuItemView.setOnClickListener {
            Log.d("LessonsFragment", "Menu item clicked: $title")
            onMenuItemClicked(menuId)
        }

        return menuItemView
    }

    private fun onMenuItemClicked(itemId: Int) {
        when (itemId) {
            R.id.nav_profile -> {
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_friends -> {
                val intent = Intent(requireContext(), FriendsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_cards -> {
                val intent = Intent(requireContext(), MyCardsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_top_learner -> {
                val intent = Intent(requireContext(), TopLearnerActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_trenning -> {
                LessonState.id = 0
                val intent = Intent(requireContext(), TrainingMyCardsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_settings -> {
                val intent = Intent(requireContext(), SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_note -> {
                val intent = Intent(requireContext(), NoteEditorActivity::class.java)
                startActivity(intent)
            }
        }

        // Закрываем drawer если нужно
        drawerLayout.closeDrawers()
    }

    // Остальные методы остаются без изменений...
    fun getLessonNumber(callback: (Int?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("Users/$userId/Stats_YomuCards/LessonNumber")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lessonNumber = snapshot.getValue(Int::class.java)
                    callback(lessonNumber)
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Ошибка чтения данных: ${error.message}")
                    callback(0)
                }
            })
        } else {
            println("Ошибка: пользователь не авторизован")
            callback(null)
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = SnakeLayoutManager()
        (recyclerView.layoutManager as SnakeLayoutManager).s = 0
        adapter = LessonsAdapter(emptyList(), parentFragmentManager)
        recyclerView.adapter = adapter
    }

    private fun checkDatabaseState() {
        if (databaseHelper.isDatabaseAvailable) {
            loadData()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                checkDatabaseState()
            }, 200)
        }
    }

    private fun loadData() {
        Thread {
            val lessons = loadLessonsFromDB(requireContext())

            requireActivity().runOnUiThread {
                adapter.updateData(lessons)
                Log.d("Lesson", "${lessons[0].LessonID},${lessons[1].LessonID},${lessons[2].LessonID},${lessons[3].LessonID},${lessons[4].LessonID}")
            }
        }.start()
    }

    private fun getDatabase(context: Context): SQLiteDatabase {
        val databasePath = File(context.filesDir, "yomucardsdb.db").absolutePath
        return SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun loadLessonsFromDB(context: Context): List<Lesson> {
        val lessons = mutableListOf<Lesson>()
        val db = getDatabase(context)

        db.rawQuery("SELECT * FROM Lessons", null).use { lessonsCursor ->
            while (lessonsCursor.moveToNext()) {
                val lessonId = lessonsCursor.getInt(lessonsCursor.getColumnIndexOrThrow("LessonID"))
                val characterIdsJson = lessonsCursor.getString(lessonsCursor.getColumnIndexOrThrow("CharacterIDs"))

                val characterIds = characterIdsJson
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().toInt() }

                val kanas = characterIds.mapNotNull { loadKanaForId(db, it) }

                lessons.add(Lesson().apply {
                    LessonID = lessonId
                    Items = kanas
                })
            }
        }

        return lessons
    }

    private fun loadKanaForId(db: SQLiteDatabase, characterId: Int): Kana? {
        val unifiedCursor = db.rawQuery("""
        SELECT CharacterID, Type 
        FROM UnifiedCharacters 
        WHERE ID = ?
    """, arrayOf(characterId.toString()))

        return unifiedCursor.use {
            if (!it.moveToFirst()) return null

            val unifiedId = it.getInt(it.getColumnIndexOrThrow("CharacterID"))
            val typeStr = it.getString(it.getColumnIndexOrThrow("Type"))
            val type = when (typeStr) {
                "Hiragana" -> KanaType.HIRAGANA
                "Katakana" -> KanaType.KATAKANA
                "Kanji" -> KanaType.KANJI
                else -> return null
            }

            when (type) {
                KanaType.HIRAGANA -> loadHiragana(db, unifiedId)
                KanaType.KATAKANA -> loadKatakana(db, unifiedId)
                KanaType.KANJI -> loadKanji(db, unifiedId)
            }
        }
    }

    private fun loadHiragana(db: SQLiteDatabase, id: Int): Kana {
        return db.rawQuery("""
        SELECT kana, ENtranscription, RUtranscription 
        FROM Hiragana 
        WHERE ID = ?
    """, arrayOf(id.toString())).use { cursor ->
            cursor.moveToFirst()
            Kana(
                id = id,
                symbol = cursor.getString(0),
                pronunciation = cursor.getString(1),
                ruTranscription = cursor.getString(2),
                ruTranslation = null,
                type = KanaType.HIRAGANA
            )
        }
    }

    private fun loadKatakana(db: SQLiteDatabase, id: Int): Kana {
        return db.rawQuery("""
        SELECT kana, ENtranscription, RUtranscription 
        FROM Katakana 
        WHERE ID = ?
    """, arrayOf(id.toString())).use { cursor ->
            cursor.moveToFirst()
            Kana(
                id = id,
                symbol = cursor.getString(0),
                pronunciation = cursor.getString(1),
                ruTranscription = cursor.getString(2),
                ruTranslation = null,
                type = KanaType.KATAKANA
            )
        }
    }

    private fun loadKanji(db: SQLiteDatabase, id: Int): Kana {
        return db.rawQuery("""
        SELECT kanj, ENtranscription, RUtranscription, RUtranslation 
        FROM Kanji 
        WHERE ID = ?
    """, arrayOf(id.toString())).use { cursor ->
            cursor.moveToFirst()
            Kana(
                id = id,
                symbol = cursor.getString(0),
                pronunciation = cursor.getString(1),
                ruTranscription = cursor.getString(2),
                ruTranslation = cursor.getString(3),
                type = KanaType.KANJI
            )
        }
    }
}