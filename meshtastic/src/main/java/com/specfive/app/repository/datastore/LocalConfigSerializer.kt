package com.specfive.app.repository.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.specfive.app.LocalOnlyProtos.LocalConfig
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer for the [LocalConfig] object defined in localonly.proto.
 */
@Suppress("BlockingMethodInNonBlockingContext")
object LocalConfigSerializer : Serializer<LocalConfig> {
    override val defaultValue: LocalConfig = LocalConfig.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LocalConfig {
        try {
            return LocalConfig.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: LocalConfig, output: OutputStream) = t.writeTo(output)
}
