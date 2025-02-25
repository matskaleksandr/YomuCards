package com.QuQ.yomucards

data class UserF(
    val id: String,
    val username: String,
    val avatarPath: String,
    var isFriendRequestSent: Boolean = false, // Заявка отправлена
    var isFriend: Boolean = false // Уже в друзьях
)