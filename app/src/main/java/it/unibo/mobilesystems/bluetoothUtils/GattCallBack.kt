package it.unibo.mobilesystems.bluetoothUtils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import it.unibo.mobilesystems.debugUtils.Debugger

class GattCallBack(private val func: (int: Int) -> Unit): BluetoothGattCallback() {

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Debugger.printDebug("GATT RSSI", "rssi is : $rssi")
            func.invoke(rssi)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                Debugger.printDebug("GATT", "Gatt Connected!")
                Debugger.printDebug("GATT", "ReadRssi(): ${gatt?.readRemoteRssi()}")
                Thread(PeriodicRSSIReader(gatt)).start()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                Debugger.printDebug("GATT", "Gatt Disconnected!")
            }
        }



 class PeriodicRSSIReader(val gatt: BluetoothGatt?) : Runnable {
     override fun run() {
         Thread.sleep(4000)
         gatt?.readRemoteRssi()
     }

 }

}