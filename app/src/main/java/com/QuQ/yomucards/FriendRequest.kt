package com.QuQ.yomucards

data class FriendRequest(
    val userId: String, // ID пользователя, отправившего заявку
    val username: String, // Имя пользователя
    val avatarPath: String // Путь к аватарке
)