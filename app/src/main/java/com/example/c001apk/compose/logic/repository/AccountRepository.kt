package com.example.c001apk.compose.logic.repository

import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.dao.AccountDao
import com.example.c001apk.compose.logic.model.AccountEntity
import com.example.c001apk.compose.util.CookieUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    val allAccounts: Flow<List<AccountEntity>> = accountDao.observeAll()

    suspend fun getAllAccounts(): List<AccountEntity> = withContext(Dispatchers.IO) {
        accountDao.getAll()
    }

    suspend fun getAccount(uid: String): AccountEntity? = withContext(Dispatchers.IO) {
        accountDao.getByUid(uid)
    }

    suspend fun saveOrUpdateAccount(
        uid: String,
        username: String,
        token: String,
        userAvatar: String = EMPTY_STRING,
        level: String = EMPTY_STRING,
        experience: String = EMPTY_STRING,
        nextLevelExperience: String = EMPTY_STRING
    ) = withContext(Dispatchers.IO) {
        if (uid.isEmpty()) return@withContext
        val existing = accountDao.getByUid(uid)
        val entity = if (existing != null) {
            existing.copy(
                username = username.ifEmpty { existing.username },
                token = token.ifEmpty { existing.token },
                userAvatar = userAvatar.ifEmpty { existing.userAvatar },
                level = level.ifEmpty { existing.level },
                experience = experience.ifEmpty { existing.experience },
                nextLevelExperience = nextLevelExperience.ifEmpty { existing.nextLevelExperience },
                lastActiveTime = System.currentTimeMillis()
            )
        } else {
            AccountEntity(
                uid = uid,
                username = username,
                token = token,
                userAvatar = userAvatar,
                level = level,
                experience = experience,
                nextLevelExperience = nextLevelExperience,
                lastActiveTime = System.currentTimeMillis()
            )
        }
        accountDao.upsert(entity)
    }

    suspend fun switchAccount(uid: String) = withContext(Dispatchers.IO) {
        val target = accountDao.getByUid(uid) ?: return@withContext
        val updated = target.copy(lastActiveTime = System.currentTimeMillis())
        accountDao.upsert(updated)

        userPreferencesRepository.apply {
            setUid(updated.uid)
            setUsername(updated.username)
            setToken(updated.token)
            setUserAvatar(updated.userAvatar)
            setLevel(updated.level)
            setExperience(updated.experience)
            setNextLevelExperience(updated.nextLevelExperience)
            setIsLogin(true)
        }

        CookieUtil.isLogin = true
        CookieUtil.uid = updated.uid
        CookieUtil.username = updated.username
        CookieUtil.token = updated.token
    }

    suspend fun deleteAccount(uid: String) = withContext(Dispatchers.IO) {
        accountDao.deleteByUid(uid)
        val currentPrefs = userPreferencesRepository.data.firstOrNull()
        if (currentPrefs?.uid == uid) {
            val remaining = accountDao.getAll()
            if (remaining.isNotEmpty()) {
                val next = remaining.first()
                switchAccount(next.uid)
            } else {
                logoutCurrent()
            }
        }
    }

    suspend fun logoutCurrent() = withContext(Dispatchers.IO) {
        userPreferencesRepository.apply {
            setUid(EMPTY_STRING)
            setUserAvatar(EMPTY_STRING)
            setUsername(EMPTY_STRING)
            setToken(EMPTY_STRING)
            setLevel(EMPTY_STRING)
            setExperience(EMPTY_STRING)
            setNextLevelExperience(EMPTY_STRING)
            setIsLogin(false)
        }
        CookieUtil.isLogin = false
        CookieUtil.uid = EMPTY_STRING
        CookieUtil.username = EMPTY_STRING
        CookieUtil.token = EMPTY_STRING
    }

    suspend fun logoutAll() = withContext(Dispatchers.IO) {
        accountDao.deleteAll()
        logoutCurrent()
    }

}
