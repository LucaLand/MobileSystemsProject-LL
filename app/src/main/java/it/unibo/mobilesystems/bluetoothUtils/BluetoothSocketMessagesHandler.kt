package it.unibo.mobilesystems.bluetoothUtils

import android.os.Handler
import android.os.Looper
import android.os.Message


import it.unibo.mobilesystems.debugUtils.Debugger



class BluetoothSocketMessagesHandler: Handler(Looper.myLooper()!!) {

    var callbackMap: MutableMap<Int, (string: String?) -> Unit> = mutableMapOf()

    fun setCallbackForMessage(messageType: Int, callback: (string: String?) -> Unit){
        callbackMap[messageType] = callback
    }


    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        Debugger.printDebug("HANDLER", "Obj: ${msg.obj} - Data: ${msg.data.get("MSG")}")
        val string = msg.obj?.toString()

        val msgCode = msg.what
        callbackMap.toList().forEach {
            if (msgCode == it.first)
                it.second.invoke(string)
        }
    }
        /*
        when (msg.what) {
            MESSAGE_SOCKET_ERROR -> {
                if (enabled)
                    restartConnection()
            }
            MESSAGE_SEND_ERROR -> {
                Debugger.printDebug("HANDLER", "ERROR SENDING MESSAGE")
            }
            MESSAGE_READ -> {
                Debugger.printDebug("HANDLER", msg.obj.toString())
            }
            MESSAGE_CONNECTION_TRUE -> {
                correctlyConnected()
            }
        }
         */
}