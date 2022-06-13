package it.unibo.mobilesystems.recivers

import android.app.Notification
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ActionHandler(val action : String, val func :(context: Context?, intent: Intent?) -> Unit) {

    fun createBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                func(p0, p1)
            }
        }
    }

}