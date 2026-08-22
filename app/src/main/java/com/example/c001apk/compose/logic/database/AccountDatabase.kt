package com.example.c001apk.compose.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.c001apk.compose.logic.dao.AccountDao
import com.example.c001apk.compose.logic.model.AccountEntity

@Database(version = 1, entities = [AccountEntity::class])
abstract class AccountDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
