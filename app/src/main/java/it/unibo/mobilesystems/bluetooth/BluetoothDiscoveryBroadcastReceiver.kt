package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothDiscoveryBroadcastReceiver : BroadcastReceiver() {

    private val onDeviceDiscovered = mutableListOf<(BluetoothDevice) -> Unit>()

    fun addOnDeviceDiscovered(action : (BluetoothDevice) -> Unit) : Boolean {
        return onDeviceDiscovered.add(action)
    }

    fun removeOnDeviceDiscovered(action: (BluetoothDevice) -> Unit) : Boolean {
        return onDeviceDiscovered.remove(action)
    }

    fun removeAllOnDeviceDiscovered() {
        return onDeviceDiscovered.clear()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action!!) {
            BluetoothDevice.ACTION_FOUND -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)!!
                onDeviceDiscovered.forEach { it(device) }
            }
        }
    }



}