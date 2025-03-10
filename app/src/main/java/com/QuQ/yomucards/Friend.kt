package com.QuQ.yomucards

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Friend(
    val id: String, // ID друга
    val username: String, // Имя друга
    val avatarPath: String // Путь к аватарке
) : Parcelable
