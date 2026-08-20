package com.example.c001apk.compose.di

import android.content.Context
import androidx.room.Room
import com.example.c001apk.compose.logic.dao.AccountDao
import com.example.c001apk.compose.logic.dao.HistoryFavoriteDao
import com.example.c001apk.compose.logic.dao.HomeMenuDao
import com.example.c001apk.compose.logic.dao.RecentAtUserDao
import com.example.c001apk.compose.logic.dao.StringEntityDao
import com.example.c001apk.compose.logic.database.AccountDatabase
import com.example.c001apk.compose.logic.database.HistoryFavoriteDatabase
import com.example.c001apk.compose.logic.database.HomeMenuDatabase
import com.example.c001apk.compose.logic.database.RecentAtUserDatabase
import com.example.c001apk.compose.logic.database.StringEntityDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserBlackList

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TopicBlackList

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchHistory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RecentEmoji

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BrowseHistory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FeedFavorite

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @RecentEmoji
    @Singleton
    @Provides
    fun provideRecentEmojiDao(@RecentEmoji stringEntityDatabase: StringEntityDatabase): StringEntityDao {
        return stringEntityDatabase.stringEntityDao()
    }

    @RecentEmoji
    @Singleton
    @Provides
    fun provideRecentEmojiDatabase(@ApplicationContext context: Context): StringEntityDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            StringEntityDatabase::class.java, "recent_emoji"
        )
            .build()
    }

    @UserBlackList
    @Singleton
    @Provides
    fun provideUserBlackListDao(@UserBlackList stringEntityDatabase: StringEntityDatabase): StringEntityDao {
        return stringEntityDatabase.stringEntityDao()
    }

    @UserBlackList
    @Singleton
    @Provides
    fun provideUserBlackListDatabase(@ApplicationContext context: Context): StringEntityDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            StringEntityDatabase::class.java, "user_blacklist"
        )
            .build()
    }

    @TopicBlackList
    @Singleton
    @Provides
    fun provideTopicBlackListDao(@TopicBlackList stringEntityDatabase: StringEntityDatabase): StringEntityDao {
        return stringEntityDatabase.stringEntityDao()
    }

    @TopicBlackList
    @Singleton
    @Provides
    fun provideTopicBlackListDatabase(@ApplicationContext context: Context): StringEntityDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            StringEntityDatabase::class.java, "topic_blacklist"
        )
            .build()
    }

    @SearchHistory
    @Singleton
    @Provides
    fun provideSearchHistoryDao(@SearchHistory stringEntityDatabase: StringEntityDatabase): StringEntityDao {
        return stringEntityDatabase.stringEntityDao()
    }

    @SearchHistory
    @Singleton
    @Provides
    fun provideSearchHistoryDatabase(@ApplicationContext context: Context): StringEntityDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            StringEntityDatabase::class.java, "search_history"
        )
            .build()
    }

    @BrowseHistory
    @Singleton
    @Provides
    fun provideBrowseHistoryDao(@BrowseHistory browseHistoryDatabase: HistoryFavoriteDatabase): HistoryFavoriteDao {
        return browseHistoryDatabase.historyFavoriteDao()
    }

    @BrowseHistory
    @Singleton
    @Provides
    fun provideBrowseHistoryDatabase(@ApplicationContext context: Context): HistoryFavoriteDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HistoryFavoriteDatabase::class.java, "browse_history"
        ).build()
    }

    @FeedFavorite
    @Singleton
    @Provides
    fun provideFeedFavoriteDao(@FeedFavorite feedFavoriteDatabase: HistoryFavoriteDatabase): HistoryFavoriteDao {
        return feedFavoriteDatabase.historyFavoriteDao()
    }

    @FeedFavorite
    @Singleton
    @Provides
    fun provideFeedFavoriteDatabase(@ApplicationContext context: Context): HistoryFavoriteDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HistoryFavoriteDatabase::class.java, "feed_favorite"
        )
            .build()
    }

    @Singleton
    @Provides
    fun provideHomeMenuDao(homeMenuDatabase: HomeMenuDatabase): HomeMenuDao {
        return homeMenuDatabase.homeMenuDao()
    }

    @Singleton
    @Provides
    fun provideHomeMenuDatabase(@ApplicationContext context: Context): HomeMenuDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HomeMenuDatabase::class.java, "home_menu"
        )
            .build()
    }

    @Singleton
    @Provides
    fun provideRecentAtUserDao(recentAtUserDatabase: RecentAtUserDatabase): RecentAtUserDao {
        return recentAtUserDatabase.recentAtUserDao()
    }

    @Singleton
    @Provides
    fun provideRecentAtUserDatabase(@ApplicationContext context: Context): RecentAtUserDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RecentAtUserDatabase::class.java, "recent_at_user"
        )
            .build()
    }

    @Singleton
    @Provides
    fun provideAccountDao(accountDatabase: AccountDatabase): AccountDao {
        return accountDatabase.accountDao()
    }

    @Singleton
    @Provides
    fun provideAccountDatabase(@ApplicationContext context: Context): AccountDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AccountDatabase::class.java, "account_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

}