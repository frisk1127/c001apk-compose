package com.example.c001apk.compose.logic.repository

import androidx.lifecycle.LiveData
import com.example.c001apk.compose.di.TopicBlackList
import com.example.c001apk.compose.di.UserBlackList
import com.example.c001apk.compose.logic.dao.StringEntityDao
import com.example.c001apk.compose.logic.model.StringEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlackListRepo @Inject constructor(
    @param:UserBlackList
    private val userBlackListDao: StringEntityDao,
    @param:TopicBlackList
    private val topicBlackListDao: StringEntityDao,
) {

    fun loadAllUserListLive(): LiveData<List<StringEntity>> {
        return userBlackListDao.loadAllListLive()
    }

    fun loadAllUserListFlow(): Flow<List<StringEntity>> {
        return userBlackListDao.loadAllListFlow()
    }

    suspend fun insertUid(uid: String) {
        userBlackListDao.insert(StringEntity(uid))
    }

    suspend fun insertUidList(list: List<StringEntity>) {
        userBlackListDao.insertList(list)
    }

    suspend fun checkUid(uid: String): Boolean {
        return userBlackListDao.isExist(uid)
    }

    suspend fun saveUid(uid: String) {
        if (!userBlackListDao.isExist(uid)) {
            userBlackListDao.insert(StringEntity(uid))
        }
    }

    suspend fun deleteUid(uid: String) {
        userBlackListDao.delete(uid)
    }

    suspend fun deleteAllUser() {
        userBlackListDao.deleteAll()
    }

    fun loadAllTopicListLive(): LiveData<List<StringEntity>> {
        return topicBlackListDao.loadAllListLive()
    }

    fun loadAllTopicListFlow(): Flow<List<StringEntity>> {
        return topicBlackListDao.loadAllListFlow()
    }

    suspend fun insertTopic(topic: String) {
        topicBlackListDao.insert(StringEntity(topic))
    }

    suspend fun insertTopicList(list: List<StringEntity>) {
        topicBlackListDao.insertList(list)
    }

    suspend fun checkTopic(topic: String): Boolean {
        return withContext(Dispatchers.IO) {
            topicBlackListDao.isContain(topic)
        }
    }

    suspend fun saveTopic(topic: String) {
        if (!topicBlackListDao.isExist(topic)) {
            topicBlackListDao.insert(StringEntity(topic))
        }
    }

    suspend fun deleteTopic(topic: String) {
        topicBlackListDao.delete(topic)
    }

    suspend fun deleteAllTopic() {
        topicBlackListDao.deleteAll()
    }

}
