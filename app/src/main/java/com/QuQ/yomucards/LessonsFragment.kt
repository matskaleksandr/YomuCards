package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
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


    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        // Обновляем TextView с именем пользователя
        view?.findViewById<TextView>(R.id.textViewName)?.text = User.name ?: "Unknown"
        view?.findViewById<ImageView>(R.id.imageView)?.setImageBitmap(User.imageProfile)

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
                    callback(0) // В случае ошибки возвращаем null
                }
            })
        } else {
            println("Ошибка: пользователь не авторизован")
            callback(null)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                R.id.nav_cards -> {
                    val intent = Intent(context, MyCardsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_trenning -> {
                    LessonState.id = 0
                    val intent = Intent(context, TrainingMyCardsActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_settings -> {
                    val intent = Intent(context, SettingsActivity::class.java)
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

    private fun setupRecyclerView() {
        recyclerView.layoutManager = SnakeLayoutManager()
        adapter = LessonsAdapter(emptyList(),parentFragmentManager)
        recyclerView.adapter = adapter
    }




    private fun checkDatabaseState() {
        if (databaseHelper.isDatabaseAvailable) {
            //Toast.makeText(requireContext(), "Загрузка уроков", Toast.LENGTH_SHORT).show()
            loadData()
        } else {
            //Toast.makeText(requireContext(), "Проверка БД", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                checkDatabaseState()
            }, 200)
        }
    }
    private fun loadData() {
        Thread {
            val lessons = loadLessonsFromDB(requireContext())

            requireActivity().runOnUiThread {
                //Toast.makeText(requireContext(), "${lessons.size}", Toast.LENGTH_SHORT).show()
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

        // Загрузка уроков
        db.rawQuery("SELECT * FROM Lessons", null).use { lessonsCursor ->
            while (lessonsCursor.moveToNext()) {
                val lessonId = lessonsCursor.getInt(lessonsCursor.getColumnIndexOrThrow("LessonID"))
                val characterIdsJson = lessonsCursor.getString(lessonsCursor.getColumnIndexOrThrow("CharacterIDs"))


                val characterIds = characterIdsJson
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().toInt() }

                // Загрузка связанных символов
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
        // Получение информации из UnifiedCharacters
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

            // Загрузка данных из соответствующей таблицы
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
        // Аналогично Hiragana
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