package com.abhi.armz.di

import android.content.Context
import androidx.room.Room
import com.abhi.armz.data.local.ArmzDatabase
import com.abhi.armz.data.local.DownloadDao
import com.abhi.armz.data.publish.ArmzStorage
import com.abhi.armz.engine.DownloadEngine
import com.abhi.armz.engine.HttpClients
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
    fun provideDownloadEngine(
        client: OkHttpClient,
        storage: ArmzStorage,
    ): DownloadEngine = DownloadEngine(
        client = client,
        log = { message -> Timber.d("engine: %s", message) },
        publisher = storage,
    )

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArmzDatabase =
        Room.databaseBuilder(
            context,
            ArmzDatabase::class.java,
            "armz.db",
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideDownloadDao(database: ArmzDatabase): DownloadDao = database.downloadDao()
}
