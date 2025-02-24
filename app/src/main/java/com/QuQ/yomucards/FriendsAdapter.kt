package com.QuQ.yomucards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class FriendsAdapter(
    private val friends: MutableList<Friend>,
    private val onProfileClick: (Friend) -> Unit // Лямбда для перехода в профиль
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.bind(friend)
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendAvatar: ShapeableImageView = itemView.findViewById(R.id.friendAvatar)
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendButton: Button = itemView.findViewById(R.id.friendButton)

        fun bind(friend: Friend) {
            // Устанавливаем имя друга
            friendName.text = friend.username

            // Загружаем аватарку
            Glide.with(itemView.context)
                .load(friend.avatarPath)
                .placeholder(R.drawable.ic_yo)
                .error(R.drawable.ic_yo)
                .into(friendAvatar)

            // Обработка нажатия на кнопку "Профиль"
            friendButton.setOnClickListener {
                onProfileClick(friend)
            }
        }
    }
}