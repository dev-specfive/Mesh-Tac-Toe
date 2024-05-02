package com.spark.app.database

import android.app.Application
import com.spark.app.database.dao.MeshLogDao
import com.spark.app.database.dao.NodeInfoDao
import com.spark.app.database.dao.PacketDao
import com.spark.app.database.dao.QuickChatActionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(app: Application): MeshtasticDatabase =
        MeshtasticDatabase.getDatabase(app)

    @Provides
    fun provideNodeInfoDao(database: MeshtasticDatabase): NodeInfoDao {
        return database.nodeInfoDao()
    }

    @Provides
    fun providePacketDao(database: MeshtasticDatabase): PacketDao {
        return database.packetDao()
    }

    @Provides
    fun provideMeshLogDao(database: MeshtasticDatabase): MeshLogDao {
        return database.meshLogDao()
    }

    @Provides
    fun provideQuickChatActionDao(database: MeshtasticDatabase): QuickChatActionDao {
        return database.quickChatActionDao()
    }
}