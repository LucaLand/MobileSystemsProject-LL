package it.unibo.mobilesystems.bluetoothUtils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import it.unibo.mobilesystems.debugUtils.Debugger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

private const val TAG = "MY_APP_DEBUG_TAG"

// Defines several constants used when transmitting messages between the
// service and the UI.
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
const val MESSAGE_SOCKET_ERROR: Int = 154
const val MESSAGE_SEND_ERROR: Int = 99
const val MESSAGE_CONNECTION_TRUE = 74
// ... (Add other message types here as needed.)


private const val CLASS_NAME = "SERVICE-CLASS"
object MyBluetoothService{
    private const val maximumConnectionRetry = 30
    lateinit var connectionThread: BluetoothSocketThread
    var enabled = false
    var numberOfConnectionTried: Int = 0

    lateinit var handler: Handler
    lateinit var mac: String
    lateinit var uuid: UUID
    lateinit var bluetoothAdapter: BluetoothAdapter


    fun setServiceBluetoothAdapter(bluetoothAdapter: BluetoothAdapter){
        this.bluetoothAdapter = bluetoothAdapter
    }

    fun setDevice(macAddress: String, uuid: UUID){
        this.mac = macAddress
        this.uuid = uuid
    }

    fun setServiceHandler(handler: Handler){
        this.handler = handler
    }

    fun startSocketConnection(){
        bluetoothAdapter.cancelDiscovery()
        connectionThread = BluetoothSocketThread(
            bluetoothAdapter,
            mac,
            uuid
        )
        connectionThread.start()
    }

    //---------------------------------
    fun restartConnection(){
        if(numberOfConnectionTried <= maximumConnectionRetry) {
            if (connectionThread.isAlive) {
                if(connectionThread.socketCreated)
                    connectionThread.cancel()
                connectionThread.interrupt()
            }
            connectionThread = BluetoothSocketThread(bluetoothAdapter, mac, uuid)
            connectionThread.start()
            numberOfConnectionTried++
        }
        Debugger.printDebug(CLASS_NAME, "Retriing N.$numberOfConnectionTried")
    }

    fun stopService(){
        enabled = false
    }

    fun sendMsg(s : String){
        Debugger.printDebug(TAG, "Trying Send: msg[$s]")
        if(enabled)
            connectionThread.write(s.toByteArray())
    }



    /** ------------------------------------------------------------------------------ **/

    class BluetoothSocketThread(private var bluetoothAdapter: BluetoothAdapter,
                                      private var mac: String, private var uuid: UUID
    ) : Thread(){

        lateinit var bluetoothSocket : BluetoothSocket
        private set

        lateinit var mmInStream: InputStream
        lateinit var mmOutStream: OutputStream
        lateinit var mmBuffer: ByteArray

        var socketCreated = false

        private fun initSocket(){
            bluetoothSocket = createSocket(bluetoothAdapter, mac, uuid)
            mmInStream = bluetoothSocket.inputStream
            mmOutStream = bluetoothSocket.outputStream
            mmBuffer = ByteArray(1024)
        }

        @SuppressLint("MissingPermission")
        private fun createSocket(bluetoothAdapter: BluetoothAdapter, mac: String, uuid: UUID) : BluetoothSocket{
            Debugger.printDebug("Creating Socket...")
            return bluetoothAdapter.getRemoteDevice(mac).createRfcommSocketToServiceRecord(uuid)
        }

        @SuppressLint("MissingPermission")
        private fun connectToSocket(socket: BluetoothSocket): Boolean{
            socketCreated = try{
                socket.connect()
                Debugger.printDebug("socket.connect()","CONNECTED TO SOCKET")
                val sendMessage = handler.obtainMessage(MESSAGE_CONNECTION_TRUE)
                handler.sendMessage(sendMessage)
                true
            }catch (e: IOException) {
                Debugger.printDebug("socket.connect()","ERROR: read failed, socket might closed or timeout")
                val writeErrorMsg = handler.obtainMessage(MESSAGE_SOCKET_ERROR)
                handler.sendMessage(writeErrorMsg)
                false
            }
            return socketCreated
        }

        private fun waitSomeTime(mills: Long){
            Debugger.printDebug(CLASS_NAME, "Waiting before to retry open another socket...")
            runBlocking {
                delay(mills)
            }
        }

        override fun run() {
            connectionThread.waitSomeTime(5000) //3 second first time starts
            initSocket()
            val res = connectToSocket(bluetoothSocket)
            Debugger.printDebug("Initialized Socket: Now Listening...")
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (res) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    //Log.d(TAG, "Input stream was disconnected", e)
                    val writeErrorMsg = handler.obtainMessage(MESSAGE_SOCKET_ERROR)
                    handler.sendMessage(writeErrorMsg)
                    Debugger.printDebug("Input stream was disconnected IOException in Thread.run() - Thread Closed")
                    socketCreated = false
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer.dropLast(mmBuffer.lastIndex).toByteArray().decodeToString())
                readMsg.sendToTarget()

            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
                Debugger.printDebug("Sent Message: [${bytes.decodeToString()}]")
                //Debugger.printDebug("Message Sent Correctly")
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                Debugger.printDebug("Message Send Error")
                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("MSG", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, bytes.decodeToString()) //bytes.decodeToString()
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                bluetoothSocket.close()
                Debugger.printDebug("CONNECTION CLOSED!")
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}