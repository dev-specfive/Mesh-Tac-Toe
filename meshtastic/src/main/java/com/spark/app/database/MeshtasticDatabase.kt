package com.spark.app.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.spark.app.MyNodeInfo
import com.spark.app.NodeInfo
import com.spark.app.database.dao.PacketDao
import com.spark.app.database.dao.MeshLogDao
import com.spark.app.database.dao.NodeInfoDao
import com.spark.app.database.dao.QuickChatActionDao
import com.spark.app.database.entity.MeshLog
import com.spark.app.database.entity.Packet
import com.spark.app.database.entity.QuickChatAction

@Database(
    entities = [
        MyNodeInfo::class,
        NodeInfo::class,
        Packet::class,
        MeshLog::class,
        QuickChatAction::class
    ],
    autoMigrations = [
        AutoMigration (from = 3, to = 4),
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MeshtasticDatabase : RoomDatabase() {
    abstract fun nodeInfoDao(): NodeInfoDao
    abstract fun packetDao(): PacketDao
    abstract fun meshLogDao(): MeshLogDao
    abstract fun quickChatActionDao(): QuickChatActionDao

    companion object {
        fun getDatabase(context: Context): MeshtasticDatabase {

            return Room.databaseBuilder(
                context.applicationContext,
                MeshtasticDatabase::class.java,
                "meshtastic_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
