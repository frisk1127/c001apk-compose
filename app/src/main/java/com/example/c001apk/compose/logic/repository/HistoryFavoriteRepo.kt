package com.example.c001apk.compose.logic.repository

import androidx.lifecycle.LiveData
import com.example.c001apk.compose.di.BrowseHistory
import com.example.c001apk.compose.di.FeedFavorite
import com.example.c001apk.compose.logic.dao.HistoryFavoriteDao
import com.example.c001apk.compose.logic.model.FeedEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryFavoriteRepo @Inject constructor(
    @param:BrowseHistory
    private val browseHistoryDao: HistoryFavoriteDao,
    @param:FeedFavorite
    private val feedFavoriteDao: HistoryFavoriteDao,
) {

    fun loadAllHistoryListLive(): LiveData<List<FeedEntity>> {
        return browseHistoryDao.loadAllListLive()
    }

    fun loadAllHistoryListFlow(): Flow<List<FeedEntity>> {
        return browseHistoryDao.loadAllListFlow()
    }

    suspend fun insertHistory(history: FeedEntity) {
        browseHistoryDao.insert(history)
    }

    suspend fun checkHistory(id: String): Boolean {
        return browseHistoryDao.isExist(id)
    }

    suspend fun saveHistory(
        id: String,
        uid: String,
        uname: String,
        avatar: String,
        device: String,
        message: String,
        pubDate: String
    ) {
        if (!browseHistoryDao.isExist(id))
            browseHistoryDao.insert(
                FeedEntity(
                    id,
                    uid,
                    uname,
                    avatar,
                    device,
                    message,
                    pubDate
                )
            )
    }

    suspend fun deleteHistory(id: String) {
        browseHistoryDao.delete(id)
    }

    suspend fun deleteAllHistory() {
        browseHistoryDao.deleteAll()
    }

    fun loadAllFavoriteListLive(): LiveData<List<FeedEntity>> {
        return feedFavoriteDao.loadAllListLive()
    }

    fun loadAllFavoriteListFlow(): Flow<List<FeedEntity>> {
        return feedFavoriteDao.loadAllListFlow()
    }

    suspend fun insertFavorite(favorite: FeedEntity) {
        feedFavoriteDao.insert(favorite)
    }

    suspend fun checkFavorite(id: String): Boolean {
        return feedFavoriteDao.isExist(id)
    }

    suspend fun saveFavorite(
        id: String,
        uid: String,
        uname: String,
        avatar: String,
        device: String,
        message: String,
        pubDate: String
    ) {
        if (!feedFavoriteDao.isExist(id))
            feedFavoriteDao.insert(
                FeedEntity(
                    id,
                    uid,
                    uname,
                    avatar,
                    device,
                    message,
                    pubDate
                )
            )
    }

    suspend fun deleteFavorite(id: String) {
        feedFavoriteDao.delete(id)
    }

    suspend fun deleteAllFavorite() {
        feedFavoriteDao.deleteAll()
    }

    suspend fun deleteHistoryByUid(uid: String){
        browseHistoryDao.deleteByUid(uid)
    }
    
    suspend fun deleteFavByUid(uid: String){
        feedFavoriteDao.deleteByUid(uid)
    }

}
