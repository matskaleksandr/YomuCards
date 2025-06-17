package com.QuQ.yomucards

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.QuQ.yomucards.databinding.ActivityNoteEditorBinding
import android.text.Editable
import android.text.TextWatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import android.widget.ImageButton
import com.google.gson.Gson

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private var currentColor = Color.BLACK
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var textWatcher: TextWatcher
    private lateinit var btnExitNote: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        btnExitNote = findViewById(R.id.btnExitNote)

        setupColorButtons()
        setupSaveButton()
        setupColorTextWatcher()
        loadNote()
        setupExitButton()
    }

    private fun setupColorTextWatcher() {
        textWatcher = object : TextWatcher {
            private var start = 0
            private var end = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Не требуется
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                this.start = start
                this.end = start + count
            }

            override fun afterTextChanged(s: Editable?) {
                if (start < end && s != null) {
                    s.setSpan(
                        ForegroundColorSpan(currentColor),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        binding.noteEditText.addTextChangedListener(textWatcher)
    }

    private fun setupColorButtons() {
        val colorButtons = listOf(
            binding.colorBlack to Color.BLACK,
            binding.colorRed to Color.RED,
            binding.colorBlue to Color.BLUE,
            binding.colorGreen to Color.GREEN,
            binding.colorYellow to Color.YELLOW
        )

        colorButtons.forEach { (button, color) ->
            button.setOnClickListener {
                currentColor = color
                highlightSelectedColor(button)
                applyColorToSelection()
            }
        }

        highlightSelectedColor(binding.colorBlack)
    }

    private fun highlightSelectedColor(selectedButton: View) {
        listOf(
            binding.colorBlack,
            binding.colorRed,
            binding.colorBlue,
            binding.colorGreen,
            binding.colorYellow
        ).forEach { button ->
            button.background.alpha = if (button == selectedButton) 255 else 100
        }
    }

    private fun applyColorToSelection() {
        val start = binding.noteEditText.selectionStart
        val end = binding.noteEditText.selectionEnd

        if (start >= 0 && end > start) {
            val editable = binding.noteEditText.text
            editable.setSpan(
                ForegroundColorSpan(currentColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    data class ColorSpanInfo(val start: Int, val end: Int, val color: Int)
    data class NoteData(val text: String, val spans: List<ColorSpanInfo>)

    private fun loadNote() {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("Users").child(userId).child("Stats_YomuCards").child("NoteText")
            .get()
            .addOnSuccessListener { snapshot ->
                val json = snapshot.getValue(String::class.java)
                if (!json.isNullOrEmpty()) {
                    try {
                        val noteData = Gson().fromJson(json, NoteData::class.java)

                        // Временно отключаем TextWatcher
                        binding.noteEditText.removeTextChangedListener(textWatcher)

                        val spannable = SpannableStringBuilder(noteData.text)

                        noteData.spans.forEach { spanInfo ->
                            try {
                                if (spanInfo.start >= 0 && spanInfo.end <= spannable.length) {
                                    spannable.setSpan(
                                        ForegroundColorSpan(spanInfo.color),
                                        spanInfo.start,
                                        spanInfo.end,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    Log.d("LoadNote", "Applied color ${spanInfo.color} from ${spanInfo.start} to ${spanInfo.end}")
                                } else {
                                    Log.w("LoadNote", "Invalid span range: ${spanInfo.start}-${spanInfo.end} for text length ${spannable.length}")
                                }
                            } catch (e: Exception) {
                                Log.e("LoadNote", "Error applying span: ${e.message}")
                            }
                        }

                        binding.noteEditText.setText(spannable)

                        // Включаем TextWatcher обратно
                        binding.noteEditText.addTextChangedListener(textWatcher)
                    } catch (e: Exception) {
                        Log.e("LoadNote", "Error parsing note data: ${e.message}")
                        Snackbar.make(binding.root, "Ошибка загрузки заметки", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Ошибка загрузки: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun saveNote() {
        val text = binding.noteEditText.text.toString()
        val spans = binding.noteEditText.text.getSpans(0, text.length, ForegroundColorSpan::class.java)

        val colorSpanInfos = spans.map {
            ColorSpanInfo(
                binding.noteEditText.text.getSpanStart(it),
                binding.noteEditText.text.getSpanEnd(it),
                it.foregroundColor
            )
        }

        val noteData = NoteData(text, colorSpanInfos)
        val json = Gson().toJson(noteData)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.reference.child("Users").child(userId).child("Stats_YomuCards").child("NoteText")
                .setValue(json)
                .addOnSuccessListener {
                    Snackbar.make(binding.root, "Заметка сохранена", Snackbar.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Snackbar.make(binding.root, "Ошибка сохранения: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveNote()
        }
    }

    private fun setupExitButton() {
        btnExitNote.setOnClickListener {
            saveNote()
            finish()
        }
    }
}