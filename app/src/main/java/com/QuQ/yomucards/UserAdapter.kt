package com.QuQ.yomucards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage

class UserAdapter(private val userList: List<UserF>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)
        private val removeFriendButton: Button = itemView.findViewById(R.id.removeFriendButton)

        fun bind(user: UserF) {
            // Устанавливаем имя пользователя
            usernameTextView.text = user.username

            // Загружаем аватарку по полному URL
            if (user.avatarPath.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.avatarPath)
                    .placeholder(R.drawable.ic_yo)
                    .error(R.drawable.ic_yo)
                    .into(userAvatar)
            } else {
                // Если путь к аватарке пуст, используем заглушку
                userAvatar.setImageResource(R.drawable.ic_yo)
            }

            // Обновляем состояние кнопки
            when {
                user.isFriend -> {
                    removeFriendButton.isEnabled = false
                    removeFriendButton.text = "Друзья"
                }
                user.isFriendRequestSent -> {
                    removeFriendButton.isEnabled = false
                    removeFriendButton.text = "Отправлено"
                }
                else -> {
                    removeFriendButton.isEnabled = true
                    removeFriendButton.text = "Добавить"
                }
            }

            // Обработка нажатия на кнопку "Добавить"
            removeFriendButton.setOnClickListener {
                // Отправляем заявку в друзья
                sendFriendRequest(user)
                user.isFriendRequestSent = true // Обновляем состояние
                removeFriendButton.isEnabled = false
                removeFriendButton.text = "Отправлено"
            }
        }

        private fun sendFriendRequest(user: UserF) {
            // Логика отправки заявки в Firebase
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val database = Firebase.database.reference
                val friendRequestPath = "Users/${user.id}/friend_requests/${currentUser.uid}"
                database.child(friendRequestPath).setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(itemView.context, "Заявка отправлена", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(itemView.context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}