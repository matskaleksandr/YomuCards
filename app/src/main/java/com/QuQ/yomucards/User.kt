package com.QuQ.yomucards

data class UserF(
    val id: String,
    val username: String,
    val avatarPath: String,
    var isFriendRequestSent: Boolean = false
)