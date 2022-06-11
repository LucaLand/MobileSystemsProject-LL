package it.unibo.mobilesystems.recivers

import android.app.Notification
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent

class ActionHandler(val action : String, val func :(context: Context?, intent: Intent?) -> Unit) {
}