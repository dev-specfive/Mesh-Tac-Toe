package com.specfive.app.repository.radio

import com.specfive.app.android.Logging
import com.specfive.app.repository.bluetooth.BluetoothRepository
import com.specfive.app.util.anonymize
import javax.inject.Inject

/**
 * Bluetooth backend implementation.
 */
class BluetoothInterfaceSpec @Inject constructor(
    private val factory: BluetoothInterfaceFactory,
    private val bluetoothRepository: BluetoothRepository,
): InterfaceSpec<BluetoothInterface>, Logging {
    override fun createInterface(rest: String): BluetoothInterface {
        return factory.create(rest)
    }

    /** Return true if this address is still acceptable. For BLE that means, still bonded */
    override fun addressValid(rest: String): Boolean {
        val allPaired = bluetoothRepository.state.value.bondedDevices
            .map { it.address }.toSet()
        return if (!allPaired.contains(rest)) {
            warn("Ignoring stale bond to ${rest.anonymize}")
            false
        } else
            true
    }
}