package com.specfive.app.database

import androidx.room.TypeConverter
import com.specfive.app.DataPacket
import com.specfive.app.MeshProtos.MeshPacket
import com.google.protobuf.TextFormat
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun dataFromString(value: String): DataPacket {
        val json = Json { isLenient = true }
        return json.decodeFromString(DataPacket.serializer(), value)
    }

    @TypeConverter
    fun dataToString(value: DataPacket): String {
        val json = Json { isLenient = true }
        return json.encodeToString(DataPacket.serializer(), value)
    }

    @TypeConverter
    fun protoFromString(value: String): MeshPacket {
        val builder = MeshPacket.newBuilder()
        TextFormat.getParser().merge(value, builder)
        return builder.build()
    }

    @TypeConverter
    fun protoToString(value: MeshPacket): String {
        return value.toString()
    }
}
