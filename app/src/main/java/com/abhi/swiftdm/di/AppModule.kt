package com.abhi.swiftdm.di

import android.content.Context
import androidx.room.Room
import com.abhi.swiftdm.data.local.DownloadDao
import com.abhi.swiftdm.data.local.SwiftDmDatabase
import com.abhi.swiftdm.engine.DownloadEngine
import com.abhi.swiftdm.engine.HttpClients
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = HttpClients.shared

    @Provides
    @Singleton
    fun provideDownloadEngine(client: OkHttpClient): DownloadEngine =
        DownloadEngine(client = client, log = { message -> Timber.d("engine: %s", message) })

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SwiftDmDatabase =
        Room.databaseBuilder(
            context,
            SwiftDmDatabase::class.java,
            "swiftdm.db",
        ).build()

    @Provides
    fun provideDownloadDao(database: SwiftDmDatabase): DownloadDao = database.downloadDao()
}
