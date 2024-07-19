package com.specfive.app.service

import com.specfive.app.MeshProtos
import com.specfive.app.MeshUser
import com.specfive.app.NodeInfo
import com.specfive.app.Position
import org.junit.Assert
import org.junit.Test


class MeshServiceTest {
    val model = MeshProtos.HardwareModel.ANDROID_SIM
    val nodeInfo = NodeInfo(4, MeshUser("+one", "User One", "U1", model), Position(37.1, 121.1, 35, 10))

    @Test
    fun givenNodeInfo_whenUpdatingWithNewTime_thenPositionTimeIsUpdated() {

        val newerTime = 20
        updateNodeInfoTime(nodeInfo, newerTime)
        Assert.assertEquals(newerTime, nodeInfo.lastHeard)
    }
}


