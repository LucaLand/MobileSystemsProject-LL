package it.unibo.mobilesystems.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothActionReceiver(val actionHandlerList : MutableList<ActionHandler>) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        this.actionHandlerList.forEach { actionHandler ->
            if(intent?.action == actionHandler.action)
                actionHandler.func(context, intent)
        }
    }
}