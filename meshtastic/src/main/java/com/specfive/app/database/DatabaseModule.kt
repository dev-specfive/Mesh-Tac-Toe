package com.specfive.app.database

import android.app.Application
import com.specfive.app.database.dao.MeshLogDao
import com.specfive.app.database.dao.NodeInfoDao
import com.specfive.app.database.dao.PacketDao
import com.specfive.app.database.dao.QuickChatActionDao
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