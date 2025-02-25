package com.QuQ.yomucards

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle

import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var profileImage: ImageView
    private lateinit var usernameEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var changeImageButton: Button

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // Инициализация элементов интерфейса
        profileImage = findViewById(R.id.profileImage)
        usernameEditText = findViewById(R.id.usernameEditText)
        saveButton = findViewById(R.id.saveButton)
        changeImageButton = findViewById(R.id.changeImageButton)

        // Загрузка текущих данных пользователя
        loadUserData()

        // Изменение аватарки
        changeImageButton.setOnClickListener {
            openImageChooser()
        }

        val exitButton = findViewById<ImageButton>(R.id.btnExitEditProfile)
        exitButton.setOnClickListener{
            finish()
        }

        // Сохранение изменений
        saveButton.setOnClickListener {
            saveUserData()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val userRef = database.getReference("Users").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("Username").getValue(String::class.java)
                    val avatarUrl = snapshot.child("AvatarPath").getValue(String::class.java)

                    usernameEditText.setText(username)
                    if (avatarUrl != null) {
                        Glide.with(this@EditProfileActivity).load(avatarUrl).into(profileImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditProfileActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            profileImage.setImageURI(selectedImageUri)
        }
    }
    // Функция для сжатия изображения до указанных размеров
    fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }

    private fun saveUserData() {
        val uid = auth.currentUser?.uid
        val newUsername = usernameEditText.text.toString()

        if (uid != null && newUsername.isNotEmpty()) {
            // Обновление имени пользователя
            val userRef = database.getReference("Users").child(uid)
            userRef.child("Username").setValue(newUsername)

            // Обновляем имя в объекте User
            User.name = newUsername

            // Загрузка новой аватарки, если она выбрана
            if (selectedImageUri != null) {
                val storageRef = storage.reference.child("profile_images/$uid.jpg")
                storageRef.putFile(selectedImageUri!!)
                    .addOnSuccessListener {
                        // Получаем URL загруженного изображения
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Сохраняем URL в базе данных
                            userRef.child("AvatarPath").setValue(uri.toString())

                            // Загружаем изображение по URL и сохраняем его в User.image
                            loadImageIntoUser(uri.toString())
                            val imageView2: ImageView = findViewById(R.id.profileImage)
                            val drawable = imageView2.drawable

                            if (drawable is BitmapDrawable) {
                                // Получаем Bitmap из ImageView
                                val originalBitmap = drawable.bitmap

                                val resizedBitmap = resizeBitmap(originalBitmap, originalBitmap.width / 2, originalBitmap.height / 2)

                                // Сохраняем сжатый Bitmap в User.imageProfile
                                User.imageProfile = resizedBitmap

                                // Переходим в другое Activity
                                val intent = Intent(this, ProfileActivity::class.java)
                                //startActivity(intent)
                            }
                            Toast.makeText(this, "Данные успешно обновлены!", Toast.LENGTH_SHORT).show()
                            finish()

                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Ошибка загрузки аватарки", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Имя успешно изменено!", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
        }
    }

    // Функция для загрузки изображения по URL и сохранения его в User.image
    private fun loadImageIntoUser(imageUrl: String) {
        Glide.with(this)
            .asBitmap() // Загружаем изображение как Bitmap
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Сохраняем Bitmap в объекте User
                    User.imageProfile = resource



                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Очищаем ресурсы, если необходимо
                }
            })
    }
}