package com.QuQ.yomucards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.Spannable
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.net.HttpURLConnection
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit
import android.Manifest
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.work.WorkInfo
import java.util.concurrent.Executors


object LessonState {
    var id: Int = 0
    var kana: List<Kana>? = null
    var LessonNumber: Int = 0
    var MaxLesson: Int = 0
}

object User {
    var id: String = "-1"
    var name: String = "name"
    var imageProfile: Bitmap? = null
    var email: String = "email"

}

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем макет
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        } else {
            scheduleDailyNotification()
        }

        // Инициализируем элементы интерфейса
        val registerButton = findViewById<Button>(R.id.registerButton)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val googleSignInButton = findViewById<Button>(R.id.googleSignInButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val headerText: TextView = findViewById(R.id.headerText)

        // Пример раскраски заголовка с помощью SpannableString
        val text = "YomuCards"
        val spannableString = SpannableString(text)
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color7)), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 1-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color7)), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 2-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color3)), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 3-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color3)), 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 4-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color8)), 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 5-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color8)), 5, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 6-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color8)), 6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 7-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color8)), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 8-я буква
        spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.color8)), 8, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)  // 9-я буква
        headerText.text = spannableString

        // Инициализация FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Настройка Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Обработчик кнопки регистрации
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            registerUser(email, password)
        }

        // Обработчик кнопки входа по email
        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            signInUser(email, password)
        }

        // Обработчик кнопки входа через Google
        googleSignInButton.setOnClickListener {

            signInWithGoogle()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение предоставлено, планируем уведомления
            scheduleDailyNotification()
        } else {
            // Разрешение не предоставлено, уведомления не будут отправляться
        }
    }

    private fun requestNotificationPermission() {
        // Проверяем, есть ли разрешение
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Разрешение уже предоставлено
            scheduleDailyNotification()
        } else {
            // Запрашиваем разрешение
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun scheduleDailyNotification() {
        val workManager = WorkManager.getInstance(this)
        val workQuery = workManager.getWorkInfosByTag("daily_notification")

        workQuery.addListener({
            val existingWork = workQuery.get().find { it.state == WorkInfo.State.ENQUEUED }
            if (existingWork == null) {
                val dailyWorkRequest = PeriodicWorkRequest.Builder(
                    NotificationWorker::class.java,
                    1, TimeUnit.DAYS
                )
                    .setInitialDelay(1, TimeUnit.HOURS) // Чтобы не запускалось сразу
                    .addTag("daily_notification")
                    .build()

                workManager.enqueue(dailyWorkRequest)
            }
        }, Executors.newSingleThreadExecutor())
    }



    override fun onStart() {
        super.onStart()
        // При каждом запуске проверяем сохранённые данные входа
        val prefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val loginType = prefs.getString("loginType", null)
        if (loginType != null) {
            val loadingOverlay = findViewById<FrameLayout>(R.id.loadingOverlay)
            loadingOverlay.visibility = View.VISIBLE  // Показать
            // Чтобы выполнить повторный вход, сначала выходим
            auth.signOut()
            when (loginType) {
                "email" -> {
                    val storedEmail = prefs.getString("email", "")
                    val storedPassword = prefs.getString("password", "")
                    if (!storedEmail.isNullOrEmpty() && !storedPassword.isNullOrEmpty()) {
                        // Выполняем вход по email и паролю
                        signInUser(storedEmail, storedPassword)
                    }
                }
                "google" -> {
                    // Запускаем вход через Google
                    signInWithGoogle()
                }
            }
        }
    }

    // Функция для сохранения данных входа
    private fun storeLoginInfo(loginType: String, email: String? = null, password: String? = null) {
        val prefs = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("loginType", loginType)
            if (loginType == "email") {
                putString("email", email)
                putString("password", password)
            }
            apply()
        }
    }

    private fun registerUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) return

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Сохраняем тип входа (email) и данные
                    storeLoginInfo("email", email, password)


                    navigateToHome()
                    //Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()

                    val user = auth.currentUser
                    val uid = user?.uid
                    val nickname = user?.email

                    // Пример URL для аватара
                    val avatarPath = "https://yt3.ggpht.com/a/AATXAJzdmRM10P6trPdRbMeGM7BVbYUMdhbgtWqiUw=s900-c-k-c0xffffffff-no-rj-mo"
                    if (uid != null) {
                        val userData = mapOf(
                            "Username" to nickname,
                            "Email" to email,
                            "AvatarPath" to avatarPath,
                            "Id" to uid,
                            "BackgroundNumber" to 0
                        )
                        // Обновляем глобальный объект User (предполагается, что он создан как object User)
                        User.id = uid
                        User.name = user.email.toString()
                        User.email = user.email.toString()
                        FirebaseDatabase.getInstance().getReference("Users")
                            .child(uid)
                            .updateChildren(userData)
                            .addOnCompleteListener { dbTask ->
                                if (!dbTask.isSuccessful) {
                                    dbTask.exception?.printStackTrace()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) return
        val loadingOverlay = findViewById<FrameLayout>(R.id.loadingOverlay)
        loadingOverlay.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Сохраняем тип входа (email) и данные
                    storeLoginInfo("email", email, password)
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
                        databaseRef.get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                User.id = uid
                                User.name = snapshot.child("Username").getValue(String::class.java).orEmpty()
                                User.email = email.toString()
                                navigateToHome()
                                //Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                            } else {
                                loadingOverlay.visibility = View.GONE
                                Toast.makeText(this, "Ошибка: данные пользователя не найдены", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            loadingOverlay.visibility = View.GONE
                            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        loadingOverlay.visibility = View.GONE
                        Toast.makeText(this, "Ошибка: не найден UID", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val loadingOverlay = findViewById<FrameLayout>(R.id.loadingOverlay)
            loadingOverlay.visibility = View.VISIBLE
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            account?.let {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { authTask ->
                        if (authTask.isSuccessful) {
                            // Сохраняем тип входа (google)
                            storeLoginInfo("google")
                            val user = auth.currentUser
                            val uid = user?.uid
                            // Пример URL для аватара
                            val avatarPath = "https://yt3.ggpht.com/a/AATXAJzdmRM10P6trPdRbMeGM7BVbYUMdhbgtWqiUw=s900-c-k-c0xffffffff-no-rj-mo"
                            if (uid != null) {

                                User.id = uid
                                //User.name = account.displayName.toString()
                                User.email = account.email.toString()

                                val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)


                                // Сначала считываем текущие данные
                                userRef.get().addOnSuccessListener { snapshot ->
                                    val updates = mutableMapOf<String, Any>()

                                    if (!snapshot.hasChild("Username") && account.displayName != null) {
                                        updates["Username"] = account.displayName!!
                                    }
                                    val username = snapshot.child("Username").getValue(String::class.java)
                                    User.name = username.toString()
                                    // Аналогично для Email
                                    if (!snapshot.hasChild("Email") && account.email != null) {
                                        updates["Email"] = account.email!!
                                    }
                                    // Если AvatarPath отсутствует — добавляем URL аватара
                                    if (!snapshot.hasChild("AvatarPath")) {
                                        updates["AvatarPath"] = avatarPath
                                    }
                                    // Если поле Id отсутствует — добавляем UID
                                    if (!snapshot.hasChild("Id")) {
                                        updates["Id"] = uid
                                    }
                                    // Если поле BackgroundNumber отсутствует — устанавливаем значение 0
                                    if (!snapshot.hasChild("BackgroundNumber")) {
                                        updates["BackgroundNumber"] = 0
                                    }

                                    if (updates.isNotEmpty()) {
                                        userRef.updateChildren(updates)
                                            .addOnCompleteListener { dbTask ->
                                                if (dbTask.isSuccessful) {
                                                    navigateToHome()
                                                    //Toast.makeText(this, "Вход через Google выполнен!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    loadingOverlay.visibility = View.GONE
                                                    Toast.makeText(this, "Ошибка при записи данных: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    } else {
                                        // Если обновлять нечего — просто переходим на HomeActivity
                                        navigateToHome()
                                        //Toast.makeText(this, "Вход через Google выполнен!", Toast.LENGTH_SHORT).show()
                                    }
                                }.addOnFailureListener { error ->
                                    loadingOverlay.visibility = View.GONE
                                    Toast.makeText(this, "Ошибка при чтении данных: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                navigateToHome()
                                //Toast.makeText(this, "Вход через Google выполнен!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            loadingOverlay.visibility = View.GONE
                            Toast.makeText(this, "Ошибка: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }



    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0) // отключаем анимацию
        finish()
        overridePendingTransition(0, 0) // отключаем анимацию
    }
}





class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/quqid-a8950.appspot.com/o/YomoCards%2Fyomucardsdb.db?alt=media&token=2482566d-3c4a-4abd-aa07-64d8b16d2bfb"
    private val fileName = "yomucardsdb.db"
    lateinit var databaseHelper: DatabaseHelper


    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Игнорируем кнопку "Назад"
            }
        })

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        val file = File(filesDir, fileName)
        if(file.exists()){
            databaseHelper = DatabaseHelper(applicationContext, fileName)

            val databaseRef = FirebaseDatabase.getInstance().getReference("Versions").child("YomuCardsDB")

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Пробуем получить значение как Int
                    val value = snapshot.getValue(Int::class.java)
                    if (value != null) {
                        Log.d("FirebaseValue", "Полученное значение: $value")
                        if(value != databaseHelper.getVersionDB()){
                            file.delete()
                            databaseHelper.isDatabaseAvailable = false
                            downloadFile(downloadUrl, fileName)
                        }
                        else{
                            databaseHelper.isDatabaseAvailable = true
                        }
                        // Здесь можно использовать значение, например:
                        // val version = value
                    } else {
                        Log.w("FirebaseValue", "Значение не найдено или имеет неверный формат")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseValue", "Ошибка при получении данных: ${error.message}")
                }
            })
        }
        else{
            databaseHelper = DatabaseHelper(applicationContext, fileName)
            databaseHelper.isDatabaseAvailable = false
            downloadFile(downloadUrl, fileName)
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Переход к фрагменту "Поиск"
                    showFragment(SearchFragment())
                    true
                }

                R.id.nav_search -> {
                    // Переход к фрагменту "Уроки"
                    showFragment(LessonsFragment())
                    true
                }

                R.id.nav_profile -> {
                    // Переход к фрагменту "Переводчик"
                    showFragment(TranslatorFragment())
                    true
                }

                else -> false
            }
        }
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_search
            //showFragment(LessonsFragment())

        }



        auth = FirebaseAuth.getInstance()
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        val downloadButton = findViewById<Button>(R.id.download)
        downloadButton.setOnClickListener {
            downloadFile(downloadUrl, fileName)
        }

        signOutButton.setOnClickListener {
            auth.signOut()
            val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
            googleSignInClient.signOut().addOnCompleteListener {
                finish()
                overridePendingTransition(0, 0) // отключаем анимацию
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(0, 0) // отключаем анимацию
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        // Меняем текущий фрагмент
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null) // Если нужно, добавляем в стек
        transaction.commit()

    }

    private fun downloadFile(url: String, fileName: String) {
        // Запускаем корутину для выполнения задачи в фоновом потоке
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Переносим сетевой запрос в фоновый поток
                    downloadFileFromUrl(url, fileName)
                }

                if (result) {
                    Toast.makeText(this@HomeActivity, "Иероглифы загружены!", Toast.LENGTH_SHORT).show()
                    databaseHelper.isDatabaseAvailable = true
                } else {
                    //Toast.makeText(this@HomeActivity, "Ошибка загрузки.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun downloadFileFromUrl(url: String, fileName: String): Boolean {
        try {


            val urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connect()

            val responseCode = urlConnection.responseCode
            Log.d("Download", "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream: InputStream = urlConnection.inputStream
                // Убедимся, что директория существует
                val file = File(filesDir, fileName)

                // Проверка наличия файла
                if (file.exists()) {
                    // Проверка размера файла
                    val fileSize = file.length()
                    if (fileSize > 0) {
                        Log.d("Download", "Файл существует, размер: $fileSize байт")
                        return true
                    } else {
                        Log.e("Download", "Файл существует, но его размер равен 0 байт")
                        return false
                    }
                } else {
                    // Создаем директорию, если не существует
                    if (!file.parentFile.exists()) {
                        file.parentFile.mkdirs()  // Создаем директорию
                    }
                    val outputStream = FileOutputStream(file)

                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } != -1) {
                        outputStream.write(buffer, 0, length)
                    }

                    outputStream.close()
                    inputStream.close()

                    // Логирование пути сохранения файла
                    Log.d("Download", "Файл сохранен по пути: ${file.absolutePath}")
                    return true
                }
            } else {
                Log.e("Download", "Ошибка ответа: $responseCode")
                return false
            }
        } catch (e: Exception) {
            Log.e("Download", "Ошибка загрузки: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}





