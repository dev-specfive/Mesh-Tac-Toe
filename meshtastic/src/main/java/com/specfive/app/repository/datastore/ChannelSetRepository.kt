package com.specfive.app.repository.datastore

import androidx.datastore.core.DataStore
import com.specfive.app.android.Logging
import com.specfive.app.AppOnlyProtos.ChannelSet
import com.specfive.app.ChannelProtos.Channel
import com.specfive.app.ChannelProtos.ChannelSettings
import com.specfive.app.ConfigProtos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import javax.inject.Inject

/**
 * Class that handles saving and retrieving [ChannelSet] data.
 */
class ChannelSetRepository @Inject constructor(
    private val channelSetStore: DataStore<ChannelSet>
) : Logging {
    val channelSetFlow: Flow<ChannelSet> = channelSetStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                errormsg("Error reading DeviceConfig settings: ${exception.message}")
                emit(ChannelSet.getDefaultInstance())
            } else {
                throw exception
            }
        }

    suspend fun clearChannelSet() {
        channelSetStore.updateData { preference ->
            preference.toBuilder().clear().build()
        }
    }

    suspend fun clearSettings() {
        channelSetStore.updateData { preference ->
            preference.toBuilder().clearSettings().build()
        }
    }

    suspend fun addAllSettings(settingsList: List<ChannelSettings>) {
        channelSetStore.updateData { preference ->
            preference.toBuilder().addAllSettings(settingsList).build()
        }
    }

    /**
     * Updates the [ChannelSettings] list with the provided channel.
     */
    suspend fun updateChannelSettings(channel: Channel) {
        if (channel.role == Channel.Role.DISABLED) return
        channelSetStore.updateData { preference ->
            val builder = preference.toBuilder()
            // Resize to fit channel
            while (builder.settingsCount <= channel.index) {
                builder.addSettings(ChannelSettings.getDefaultInstance())
            }
            // use setSettings() to ensure settingsList and channel indexes match
            builder.setSettings(channel.index, channel.settings).build()
        }
    }

    suspend fun setLoraConfig(config: ConfigProtos.Config.LoRaConfig) {
        channelSetStore.updateData { preference ->
            preference.toBuilder().setLoraConfig(config).build()
        }
    }
}
