package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import kotlinx.coroutines.*

class LambdaGattCallback(
    private val coroutineScope: CoroutineScope
) : BluetoothGattCallback() {

    private val onRssiReaded = mutableListOf<(gatt : BluetoothGatt, rssi : Int, status : Int) -> Unit>()
    private val onConnectionStateChange = mutableListOf<(gatt : BluetoothGatt?,
                                                         state: Int, newStatus: Int) -> Unit>()
    private val updateUiOnRssiRead = mutableListOf<(gatt : BluetoothGatt, rssi : Int, status : Int) -> Unit>()
    private val updateUionConnectionStateChange = mutableListOf<(gatt : BluetoothGatt?,
                                                         state: Int, newStatus: Int) -> Unit>()

    fun addOnRssiReaded(updateUi: Boolean = false,
                        action : (gatt : BluetoothGatt, rssi : Int, status : Int) -> Unit) : Boolean {
        return if(updateUi) {
            updateUiOnRssiRead.add(action)
        } else {
            onRssiReaded.add(action)
        }
    }

    fun removeOnRssiReaded(action: (gatt : BluetoothGatt, rssi : Int, status : Int) -> Unit) : Boolean {
        if(onRssiReaded.remove(action))
            return true

        return updateUiOnRssiRead.remove(action)
    }

    fun addOnConnectionStateChange(updateUi : Boolean = false, action : (gatt : BluetoothGatt?,
                                   state: Int, newStatus: Int) -> Unit) : Boolean {
        return if(updateUi) {
            updateUionConnectionStateChange.add(action)
        } else {
            onConnectionStateChange.add(action)
        }
    }

    fun removeOnConnectionStateChange(action : (gatt : BluetoothGatt?,
                                             state: Int, newStatus: Int) -> Unit) : Boolean {
        if(onConnectionStateChange.remove(action))
            return true

        return updateUionConnectionStateChange.remove(action)
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        onRssiReaded.forEach { action -> action(gatt, rssi, status) }
        if(updateUiOnRssiRead.isNotEmpty())
            coroutineScope.launch(Dispatchers.Main) {
                updateUiOnRssiRead.forEach { action -> action(gatt, rssi, status) }
            }

    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newStatus: Int) {
        super.onConnectionStateChange(gatt, status, newStatus)
        onConnectionStateChange.forEach { action -> action(gatt, status, newStatus) }
        if(updateUionConnectionStateChange.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.Main) {
                updateUionConnectionStateChange.forEach { action -> action(gatt, status, newStatus) }
            }
        }
    }

}