package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import it.unibo.kactor.ApplMessage
import it.unibo.kactor.IApplMessage
import it.unibo.kactor.MsgUtil
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.IOException
import java.util.*

class QakBluetoothConnection(
    private val name : String,
    private val bluetoothDevice : BluetoothDevice,
    serviceUuid: String? = null
    ) : Closeable, AutoCloseable {

    private var bluetoothSocket : BluetoothSocket? = null
    private lateinit var input : BufferedReader
    private lateinit var output : BufferedWriter

    constructor(name : String, bluetoothDevice: BluetoothDevice, bluetoothSocket: BluetoothSocket,
                        uuid : String? = null
    ) : this(
        name, bluetoothDevice, uuid) {
        if(!bluetoothSocket.isConnected)
            throw IllegalArgumentException("socket not connected")
        this.bluetoothSocket = bluetoothSocket
    }

    val address = bluetoothDevice.address
    lateinit var serviceUuid : String
        private set

    init {
        if(serviceUuid != null)
            this.serviceUuid = serviceUuid
    }

    fun connect() {
        if(isConnected())
            throw IOException("unable to connect: already connected")

        try {
            bluetoothSocket = bluetoothDevice
                .createRfcommSocketToServiceRecord(UUID.fromString(serviceUuid))
            bluetoothSocket!!.connect()
            input = bluetoothSocket!!.inputStream.bufferedReader()
            output = bluetoothSocket!!.outputStream.bufferedWriter()
        } catch (exception : Exception) {
            bluetoothSocket = null
            throw exception
        }
    }

    fun send(msg : IApplMessage) {
        if(bluetoothSocket == null)
            throw IOException("unable to send: not connected")
        try {
            output.write(msg.toString())
            output.newLine()
            output.flush()
        } catch (e : Exception) {
            bluetoothSocket = null
            throw e
        }
    }

    fun sendDispatch(msgId : String, msgContent : String, dest : String) {
        send(MsgUtil.buildDispatch(name, msgId, msgContent, dest))
    }

    fun sendRequest(reqId : String, reqContent : String, dest : String) {
        send(MsgUtil.buildRequest(name, reqId, reqContent, dest))
    }

    fun sendReply(replyId : String, replyContent : String, dest : String) {
        send(MsgUtil.buildReply(name, replyId, replyContent, dest))
    }

    fun emit(eventId : String, eventContent : String) {
        send(MsgUtil.buildEvent(name, eventId, eventContent))
    }

    fun receive() : IApplMessage {
        if(bluetoothSocket == null)
            throw IOException("unable to receive: not connected")

        try {
            return ApplMessage(input.readLine())
        } catch (e : Exception) {
            bluetoothSocket = null
            throw e
        }
    }

    fun disonnect() {
        if(bluetoothSocket != null) {
            val oldSocket = bluetoothSocket!!
            bluetoothSocket = null
            oldSocket.close()
        }
    }

    fun isConnected() : Boolean {
        return bluetoothSocket != null
    }

    fun isDisconnected() : Boolean {
        return bluetoothSocket == null
    }

    fun setServiceUuid(serviceUuid: String) {
        if(bluetoothSocket != null)
            throw IllegalStateException("unable to set the uuid of the service while connection is active")
        this.serviceUuid = serviceUuid
    }

    override fun close() {
        disonnect()
    }
}

fun BluetoothDevice.createQakConnection(name : String, uuid : String) : QakBluetoothConnection {
    return QakBluetoothConnection(name, this, uuid)
}

fun BluetoothDevice.createQakConnection(name : String, uuid : String,
                                        withConnection: (QakBluetoothConnection) -> Unit
) : QakBluetoothConnection {
    val conn = QakBluetoothConnection(name, this, uuid)
    withConnection(conn)

    return conn
}

fun BluetoothSocket.qakConnection(name : String) : QakBluetoothConnection {
    return QakBluetoothConnection(name, this.remoteDevice, this)
}

fun BluetoothDevice.openQakConnection(name : String, uuid: String) : QakBluetoothConnection {
    val conn = QakBluetoothConnection(name, this, uuid)
    conn.connect()
    return conn
}

fun BluetoothDevice.openQakConnection(name : String, uuid: String,
                                      withConnection : (QakBluetoothConnection) -> Unit)
: QakBluetoothConnection {
    val conn = QakBluetoothConnection(name, this, uuid)
    conn.connect()
    withConnection(conn)
    return conn
}