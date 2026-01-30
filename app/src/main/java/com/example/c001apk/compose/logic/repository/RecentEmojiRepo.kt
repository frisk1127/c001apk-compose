package com.example.c001apk.compose.logic.repository

import androidx.lifecycle.LiveData
import com.example.c001apk.compose.di.RecentEmoji
import com.example.c001apk.compose.logic.dao.StringEntityDao
import com.example.c001apk.compose.logic.model.StringEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentEmojiRepo @Inject constructor(
    @param:RecentEmoji
    private val recentEmojiDao: StringEntityDao,
) {

    fun loadAllListLive(): LiveData<List<StringEntity>> {
        return recentEmojiDao.loadAllListLive()
    }

    fun loadAllListFlow(): Flow<List<StringEntity>> {
        return recentEmojiDao.loadAllListFlow()
    }

    suspend fun insertEmoji(emoji: StringEntity) {
        recentEmojiDao.insert(emoji)
    }

    suspend fun insertList(list: List<StringEntity>) {
        recentEmojiDao.insertList(list)
    }

    suspend fun saveEmoji(emoji: String) {
        if (!recentEmojiDao.isExist(emoji)) {
            recentEmojiDao.insert(StringEntity(emoji))
        }
    }

    suspend fun deleteEmoji(emoji: String) {
        recentEmojiDao.delete(emoji)
    }

    suspend fun deleteEmoji(emoji: StringEntity) {
        recentEmojiDao.delete(emoji)
    }

    suspend fun deleteAll() {
        recentEmojiDao.deleteAll()
    }

    suspend fun checkEmoji(emoji: String): Boolean {
        return recentEmojiDao.isExist(emoji)
    }

    suspend fun updateEmoji(data: String) {
        recentEmojiDao.updateHistory(data, System.currentTimeMillis())
    }

    suspend fun updateEmoji(oldData: String, newData: String) {
        recentEmojiDao.updateEmoji(oldData, newData, System.currentTimeMillis())
    }

}
