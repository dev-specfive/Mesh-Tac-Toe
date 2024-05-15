package com.spark.app.repository.radio

import java.io.Closeable

interface IRadioInterface : Closeable {
    fun handleSendToRadio(p: ByteArray)
}

