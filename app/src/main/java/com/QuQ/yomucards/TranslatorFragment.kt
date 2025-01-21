package com.QuQ.yomucards

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

class TranslatorFragment : Fragment(R.layout.fragment_translator) {
    private val API_KEY = "AQVNyBuV3Umyae4ADI_1ADqynT-9VDlRVlfml6hG"
    private val FOLDER_ID = "b1grbosn1c4l0gjtl9ul"
    private val BASE_URL = "https://translate.api.cloud.yandex.net/translate/v2/"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_translator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnTranslate = view.findViewById<Button>(R.id.btn_translate)
        val etSourceText = view.findViewById<EditText>(R.id.edt_input)
        val btnSwap = view.findViewById<Button>(R.id.btn_swap)
        val etTranslatedText = view.findViewById<TextView>(R.id.txt_output)

        btnTranslate.setOnClickListener {
            val textToTranslate = etSourceText.text.toString()
            if (textToTranslate.isNotEmpty()) {
                translateText(textToTranslate, "ru", "ja") { translatedText ->
                    etTranslatedText.text = translatedText
                }
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

    private fun translateText(text: String, sourceLang: String, targetLang: String, callback: (String) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor(API_KEY)).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(YandexTranslateService::class.java)

        val request = YandexTranslateRequest(
            folderId = FOLDER_ID,
            texts = listOf(text),
            targetLanguageCode = targetLang
        )

        service.translateText(request).enqueue(object : Callback<YandexTranslateResponse> {
            override fun onResponse(call: Call<YandexTranslateResponse>, response: retrofit2.Response<YandexTranslateResponse>) {
                if (response.isSuccessful) {
                    val translatedText = response.body()?.translations?.firstOrNull()?.text ?: "Ошибка перевода"
                    callback(translatedText)
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

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Api-Key $apiKey")
            .build()
        return chain.proceed(request)
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
