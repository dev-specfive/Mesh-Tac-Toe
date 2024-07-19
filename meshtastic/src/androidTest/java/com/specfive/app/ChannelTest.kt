package com.specfive.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.specfive.app.model.Channel
import com.specfive.app.model.URL_PREFIX
import com.specfive.app.model.getChannelUrl
import com.specfive.app.model.numChannels
import com.specfive.app.model.toChannelSet
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChannelTest {
    @Test
    fun channelUrlGood() {
        val ch = channelSet {
            settings.add(Channel.default.settings)
            loraConfig = Channel.default.loraConfig
        }
        val channelUrl = ch.getChannelUrl()

        Assert.assertTrue(channelUrl.toString().startsWith(URL_PREFIX))
        Assert.assertEquals(channelUrl.toChannelSet(), ch)
    }

    @Test
    fun channelHashGood() {
        val ch = Channel.default

        Assert.assertEquals(8, ch.hash)
    }

    @Test
    fun numChannelsGood() {
        val ch = Channel.default

        Assert.assertEquals(104, ch.loraConfig.numChannels)
    }

    @Test
    fun channelNumGood() {
        val ch = Channel.default

        Assert.assertEquals(20, ch.channelNum)
    }

    @Test
    fun radioFreqGood() {
        val ch = Channel.default

        Assert.assertEquals(906.875f, ch.radioFreq)
    }
}
