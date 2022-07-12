package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import it.unibo.mobilesystems.debugUtils.Debugger

@Deprecated("Old bluetooth system")
class GattCallBack(private val func: (int: Int) -> Unit): BluetoothGattCallback() {

    companion object{
        var gatt : BluetoothGatt? = null
        var alive : Boolean = true
    }


        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Debugger.printDebug("GATT RSSI", "rssi is : $rssi")
            GattCallBack.gatt = gatt
            func.invoke(100+rssi)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                Debugger.printDebug("GATT", "Gatt Connected!")
                Debugger.printDebug("GATT", "ReadRssi(): ${gatt?.readRemoteRssi()}")
                Thread(PeriodicRSSIReader()).start()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                Debugger.printDebug("GATT", "Gatt Disconnected!")
            }
        }



 class PeriodicRSSIReader() : Runnable {
     override fun run() {
         while(alive) {
             Thread.sleep(1000)
             gatt?.readRemoteRssi()
         }
     }

 }

}