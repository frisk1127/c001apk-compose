package com.example.c001apk.compose.logic.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.c001apk.compose.logic.model.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Upsert
    suspend fun upsertList(list: List<AccountEntity>)

    @Query("SELECT * FROM Account ORDER BY lastActiveTime DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM Account ORDER BY lastActiveTime DESC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM Account WHERE uid = :uid LIMIT 1")
    fun observeByUid(uid: String): Flow<AccountEntity?>

    @Query("SELECT * FROM Account WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): AccountEntity?

    @Query("DELETE FROM Account WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()

}
