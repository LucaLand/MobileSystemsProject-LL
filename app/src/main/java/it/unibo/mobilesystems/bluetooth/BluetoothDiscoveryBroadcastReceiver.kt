package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothDiscoveryBroadcastReceiver : BroadcastReceiver() {

    private val onDeviceDiscovered = mutableListOf<(BluetoothDevice) -> Unit>()
    private val onDiscoveryStarted = mutableListOf<() -> Unit>()
    private val onDiscoveryFinished = mutableListOf<() -> Unit>()

    fun addOnDeviceDiscovered(action : (BluetoothDevice) -> Unit) : Boolean {
        return onDeviceDiscovered.add(action)
    }

    fun removeOnDeviceDiscovered(action: (BluetoothDevice) -> Unit) : Boolean {
        return onDeviceDiscovered.remove(action)
    }

    fun removeAllOnDeviceDiscovered() {
        return onDeviceDiscovered.clear()
    }

    fun addOnDiscoveryFinished(action : () -> Unit) : Boolean {
        return onDiscoveryFinished.add(action)
    }

    fun removeOnDiscoveryFinished(action : () -> Unit) : Boolean {
        return onDiscoveryFinished.remove(action)
    }

    fun removeAllOnDiscoveryFinished() {
        return onDiscoveryFinished.clear()
    }

    fun addOnDiscoveryStarted(action : () -> Unit) : Boolean {
        return onDiscoveryStarted.add(action)
    }

    fun removeOnDiscoveryStarted(action : () -> Unit) : Boolean {
        return onDiscoveryStarted.remove(action)
    }

    fun removeAllOnDiscoveryStarted() {
        return onDiscoveryStarted.clear()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action!!) {

            BluetoothDevice.ACTION_FOUND -> {
                val device : BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                if(device != null)
                    onDeviceDiscovered.forEach { action -> action(device) }
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                onDiscoveryStarted.forEach { action -> action() }
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                onDiscoveryFinished.forEach { action -> action() }
            }
        }
    }



}