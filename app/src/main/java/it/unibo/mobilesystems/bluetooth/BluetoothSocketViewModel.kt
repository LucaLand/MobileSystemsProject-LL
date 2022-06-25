package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class BluetoothSocketViewModel(
    private val device : BluetoothDevice,
    private val serviceUuid : String
) : ViewModel() {

    fun connectSocket(onSocketConnected : suspend (BluetoothSocket) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(serviceUuid))
            socket.connect()
            onSocketConnected(socket)
        }
    }

}