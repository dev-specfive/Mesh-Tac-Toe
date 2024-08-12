package com.specfive.app.repository.radio

import java.io.Closeable

interface IRadioInterface : Closeable {
    fun handleSendToRadio(p: ByteArray)
}

