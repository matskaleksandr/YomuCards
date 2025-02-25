package com.QuQ.yomucards

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database

class KanaInfoDialogFragment(
    private val kana: Kana,
    private val kanaAdapter: KanaAdapter,
    private val kanaAdapterView: KanaAdapter.KanaViewHolder,
    private val onCardUpdated: () -> Unit,
    private val onCardRemovedListener: OnCardRemovedListener
) : DialogFragment() {

    private lateinit var btnAdd: MaterialButton
    private lateinit var green_circle: ImageView
    private var isCardAdded = false
    private lateinit var japaneseTTS: JapaneseTTS


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_kana_info, null)
        japaneseTTS = JapaneseTTS(requireContext())

        val btnPronunciation = view.findViewById<Button>(R.id.btnPronunciation)
        btnPronunciation.setOnClickListener {
            val text = kana.symbol // Текст для озвучивания
            japaneseTTS.speak(text)
        }



        btnAdd = view.findViewById(R.id.btnAdd)
        btnAdd.setOnClickListener {
            if (isCardAdded) {
                removeCardFromFirebase(kana)
            } else {
                saveCardToFirebase(kana)
            }
            onCardUpdated()
        }


        view.background = ContextCompat.getDrawable(context, R.drawable.dialog_background)

        val kanaSymbol = view.findViewById<TextView>(R.id.dialog_kana_symbol)
        val kanaPronunciation = view.findViewById<TextView>(R.id.dialog_kana_pronunciation)
        val kanaRuTranslation = view.findViewById<TextView>(R.id.dialog_kana_ru_translation)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)
        btnClose.setOnClickListener {
            dismiss() // Закрыть диалог
        }

        checkIfCardAdded(kana)

        kanaSymbol.text = kana.symbol
        kanaPronunciation.text = "Произношение: ${kana.pronunciation} / ${kana.ruTranscription}"
        kanaRuTranslation.text = "Перевод: ${kana.ruTranslation ?: "Нету"}"

        return AlertDialog.Builder(context, R.style.CustomDialogTheme) // Применяем стиль
            .setView(view)
            .create()


    }



    private fun checkIfCardAdded(kana: Kana) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val database = Firebase.database.reference
        val userId = currentUser.uid
        val cardType = when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }

        val path = "Users/$userId/Stats_YomuCards/Cards/$cardType/${kana.id}"
        database.child(path).get().addOnSuccessListener { snapshot ->
            isCardAdded = snapshot.exists() // Проверяем, существует ли запись
            btnAdd.text = if (isCardAdded) "Удалить" else "Добавить"
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Ошибка проверки: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun saveCardToFirebase(kana: Kana) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Требуется авторизация", Toast.LENGTH_SHORT).show()
            return
        }

        val database = Firebase.database.reference
        val userId = currentUser.uid
        val cardType = when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }

        val path = "Users/$userId/Stats_YomuCards/Cards/$cardType/${kana.id}"
        database.child(path).setValue(true)
            .addOnSuccessListener {
                isCardAdded = true
                btnAdd.text = "Удалить"
                val type = getCardType(kana)
                kanaAdapter.addedCards[type]?.add(kana.id.toString()) // Добавляем ID обратно
                onCardUpdated() // Обновляем UI
                onCardRemovedListener.onCardAdded(kana) // Новый метод для уведомления о добавлении
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeCardFromFirebase(kana: Kana) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Требуется авторизация", Toast.LENGTH_SHORT).show()
            return
        }

        val database = Firebase.database.reference
        val userId = currentUser.uid
        val cardType = when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }

        val path = "Users/$userId/Stats_YomuCards/Cards/$cardType/${kana.id}"
        database.child(path).removeValue()
            .addOnSuccessListener {
                isCardAdded = false
                btnAdd.text = "Добавить"
                val type = getCardType(kana)
                kanaAdapter.addedCards[type]?.remove(kana.id.toString()) // Удаляем ID
                onCardUpdated() // Обновляем UI
                onCardRemovedListener.onCardRemoved(kana)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCardType(kana: Kana): String {
        return when (kana.type) {
            KanaType.HIRAGANA -> "Hiragana"
            KanaType.KATAKANA -> "Katakana"
            KanaType.KANJI -> "Kanji"
        }
    }
}