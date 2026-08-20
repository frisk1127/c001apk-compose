package com.example.c001apk.compose.logic.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING

@Entity(tableName = "Account")
@Immutable
data class AccountEntity(
    @PrimaryKey
    val uid: String,
    val username: String = EMPTY_STRING,
    val token: String = EMPTY_STRING,
    val userAvatar: String = EMPTY_STRING,
    val level: String = EMPTY_STRING,
    val experience: String = EMPTY_STRING,
    val nextLevelExperience: String = EMPTY_STRING,
    val lastActiveTime: Long = System.currentTimeMillis()
)
