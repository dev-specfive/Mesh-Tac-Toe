package com.specfive.app.service

open class RadioNotConnectedException(message: String = "Not connected to radio") :
    BLEException(message)