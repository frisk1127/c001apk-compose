package com.example.c001apk.compose.logic.repository

import androidx.lifecycle.LiveData
import com.example.c001apk.compose.di.SearchHistory
import com.example.c001apk.compose.logic.dao.StringEntityDao
import com.example.c001apk.compose.logic.model.StringEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepo @Inject constructor(
    @param:SearchHistory
    private val searchHistoryDao: StringEntityDao,
) {

    fun loadAllListFlow(): Flow<List<StringEntity>> {
        return searchHistoryDao.loadAllListFlow()
    }

    fun loadAllListLive(): LiveData<List<StringEntity>> {
        return searchHistoryDao.loadAllListLive()
    }

    suspend fun insertHistory(history: StringEntity) {
        searchHistoryDao.insert(history)
    }

    suspend fun insertList(list: List<StringEntity>) {
        searchHistoryDao.insertList(list)
    }

    suspend fun saveHistory(history: String) {
        searchHistoryDao.insert(StringEntity(history))
    }

    suspend fun deleteHistory(history: String) {
        searchHistoryDao.delete(history)
    }

    suspend fun deleteAllHistory() {
        searchHistoryDao.deleteAll()
    }

    suspend fun isExist(history: String): Boolean {
        return searchHistoryDao.isExist(history)
    }

    suspend fun updateHistory(data: String) {
        searchHistoryDao.updateHistory(data, System.currentTimeMillis())
    }

}
