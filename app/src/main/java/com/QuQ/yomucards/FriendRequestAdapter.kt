package com.QuQ.yomucards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FriendRequestAdapter(
    private val friendRequests: MutableList<FriendRequest>,
    private val onAccept: (FriendRequest) -> Unit, // Лямбда для принятия заявки
    private val onDecline: (FriendRequest) -> Unit // Лямбда для отклонения заявки
) : RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return FriendRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val friendRequest = friendRequests[position]
        holder.bind(friendRequest)
    }

    override fun getItemCount(): Int = friendRequests.size

    fun removeRequest(position: Int) {
        friendRequests.removeAt(position)
        notifyItemRemoved(position) // Уведомляем RecyclerView об удалении элемента
    }

    inner class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.friendRequestName)
        private val userAvatar: ImageView = itemView.findViewById(R.id.friendRequestAvatar)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        private val declineButton: Button = itemView.findViewById(R.id.declineButton)



        fun bind(friendRequest: FriendRequest) {
            // Устанавливаем имя пользователя
            usernameTextView.text = friendRequest.username

            // Загружаем аватарку
            Glide.with(itemView.context)
                .load(friendRequest.avatarPath)
                .placeholder(R.drawable.ic_yo)
                .error(R.drawable.ic_yo)
                .into(userAvatar)

            // Обработка нажатия на кнопку "Принять"
            acceptButton.setOnClickListener {
                onAccept(friendRequest)
                removeRequest(adapterPosition)
            }

            // Обработка нажатия на кнопку "Отклонить"
            declineButton.setOnClickListener {
                onDecline(friendRequest)
                removeRequest(adapterPosition)
            }
        }
    }
}