package com.QuQ.yomucards

import android.content.Context
import android.content.Intent
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
import androidx.core.view.ViewCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.*
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.*





class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Найти элементы интерфейса



        setContentView(R.layout.activity_main)

        val registerButton = findViewById<Button>(R.id.registerButton)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val googleSignInButton = findViewById<Button>(R.id.googleSignInButton)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        val headerText: TextView = findViewById(R.id.headerText)
        val text = "YomuCards"
        val spannableString = SpannableString(text)

// Применяем разные цвета к каждой букве
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

        auth = FirebaseAuth.getInstance()

        // Настройка Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Регистрация
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            registerUser(email, password)
        }

        // Вход
        signInButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            signInUser(email, password)
        }

        // Вход через Google
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToHome()
        }
    }

    private fun registerUser(email: String, password: String) {
        if(email == "" || password == ""){
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                    Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInUser(email: String, password: String) {
        if(email == "" || password == ""){
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                    Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                } else {
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
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            account?.let {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { authTask ->
                        if (authTask.isSuccessful) {
                            navigateToHome()
                            Toast.makeText(this, "Вход через Google выполнен!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Ошибка: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}




class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/quqid-a8950.appspot.com/o/YomoCards%2Ftext.txt?alt=media&token=cb3618c6-cfba-48a8-a855-7dc6ffb06c7f"
    private val fileName = "test.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

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
            showFragment(SearchFragment())
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
                startActivity(Intent(this, MainActivity::class.java))
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
                    Toast.makeText(this@HomeActivity, "Загрузка завершена!", Toast.LENGTH_SHORT).show()
                    readFileAndDisplayContent(fileName)
                } else {
                    Toast.makeText(this@HomeActivity, "Ошибка загрузки.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun downloadFileFromUrl(url: String, fileName: String): Boolean {
        try {
            // Логирование URL
            Log.d("Download", "Загружаю файл с URL: $url")

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

    private fun readFileAndDisplayContent(fileName: String) {
        try {
            val file = File(filesDir, fileName)

            if (file.exists()) {
                val content = file.readText(Charsets.UTF_8)
                Log.d("FileContent", "Содержимое файла:\n$content")
                Toast.makeText(this, "Файл прочитан успешно!", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("FileContent", "Файл не найден!")
                Toast.makeText(this, "Файл не найден!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("FileContent", "Ошибка чтения файла: ${e.message}")
            Toast.makeText(this, "Ошибка чтения файла!", Toast.LENGTH_SHORT).show()
        }
    }
}


class SearchFragment : Fragment(R.layout.fragment_search) {
    // Здесь можно разместить логику для отображения "Поиск"
}

class LessonsFragment : Fragment(R.layout.fragment_lessons) {
    // Логика для отображения "Уроки"
}

class TranslatorFragment : Fragment(R.layout.fragment_translator) {
    private val API_KEY = "AQVNx-dxDf5tCG4dLEZpV7mNk6lOq89Zj8OlY3eb"  // Замени на свой API-ключ
    private val FOLDER_ID = "aje7stogrvjd11n1qb26"  // Замени на ID каталога в Yandex Cloud
    private val BASE_URL = "https://translate.api.cloud.yandex.net/translate/v2/"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_translator, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btn_translate = view.findViewById<Button>(R.id.btn_translate)
        val etSourceText = view.findViewById<EditText>(R.id.edt_input)
        val btnSwap = view.findViewById<Button>(R.id.btn_swap)
        val etTranslatedText = view.findViewById<TextView>(R.id.txt_output)

        btn_translate?.setOnClickListener {
            val textToTranslate = etSourceText.text.toString()
            if (textToTranslate.isNotEmpty()) {
                translateText(textToTranslate, "ru", "ja")
            } else {
                showToast(requireContext(), "Введите текст")
            }
        }

        btnSwap.setOnClickListener {
            val temp = etSourceText.text.toString()
            etSourceText.setText(etTranslatedText.text.toString())
            etTranslatedText.setText(temp)
        }
    }

    private fun translateText(text: String, sourceLang: String, targetLang: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(YandexTranslateService::class.java)

        val request = YandexTranslateRequest(
            folderId = FOLDER_ID,
            texts = listOf(text),
            targetLanguageCode = targetLang
        )

        service.translateText(request).enqueue(object : Callback<YandexTranslateResponse> {
            override fun onResponse(call: Call<YandexTranslateResponse>, response: Response<YandexTranslateResponse>) {
                if (response.isSuccessful) {
                    val translatedText = response.body()?.translations?.firstOrNull()?.text

                } else {
                    showToast(requireContext(), "Ошибка: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<YandexTranslateResponse>, t: Throwable) {
                showToast(requireContext(), "Ошибка соединения: ${t.message}")
            }
        })
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

interface YandexTranslateService {
    @Headers("Content-Type: application/json")
    @POST("translate")
    fun translateText(@Body request: YandexTranslateRequest): Call<YandexTranslateResponse>
}

data class YandexTranslateRequest(
    val folderId: String,
    val texts: List<String>,
    val targetLanguageCode: String
)

data class YandexTranslateResponse(
    val translations: List<Translation>
)

data class Translation(
    val text: String
)

