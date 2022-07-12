package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.util.*

object SystemConnectionCache {

    private val cache = mutableMapOf<BluetoothDevice, MutableMap<String, BluetoothSocket>>()

    private fun getOrCreate(key : BluetoothDevice) : MutableMap<String, BluetoothSocket> {
        if(!cache.containsKey(key))
            cache[key] = mutableMapOf()

        return cache[key]!!
    }

    fun putInCache(key : BluetoothDevice, uuid: String, socket : BluetoothSocket) {
        getOrCreate(key)[uuid] = socket
    }

    fun peekFromCache(key : BluetoothDevice, uuid: String) : BluetoothSocket? {
        val conns = cache[key] ?: return null
        return conns[uuid]
    }

    fun pushFromCache(key: BluetoothDevice, uuid: String) : BluetoothSocket? {
        val conns = cache[key] ?: return null
        val res = conns.remove(uuid)
        if(conns.isEmpty())
            cache.remove(key)

        return res
    }

}