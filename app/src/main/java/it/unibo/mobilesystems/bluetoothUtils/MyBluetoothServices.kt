package it.unibo.mobilesystems.bluetoothUtils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import it.unibo.mobilesystems.debugUtils.Debugger
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
// ... (Add other message types here as needed.)

class MyBluetoothService(
    // handler that gets info from Bluetooth service
    private val handler: Handler
) {

    inner class BluetoothSocketThread(bluetoothAdapter: BluetoothAdapter, mac : String, uuid: UUID) : Thread(){

        private var bluetoothAdapter = bluetoothAdapter
        private var mac = mac
        private var uuid = uuid

        private lateinit var bluetoothSocket : BluetoothSocket

        lateinit var mmInStream: InputStream
        lateinit var mmOutStream: OutputStream
        lateinit var mmBuffer: ByteArray

        private fun initSocket(){
            bluetoothSocket = createSocket(bluetoothAdapter, mac, uuid)
            mmInStream = bluetoothSocket.inputStream
            mmOutStream = bluetoothSocket.outputStream
            mmBuffer = ByteArray(1024)
        }

        @SuppressLint("MissingPermission")
        fun createSocket(bluetoothAdapter: BluetoothAdapter, mac: String, uuid: UUID) : BluetoothSocket{
            Debugger.printDebug("ASYNC NOW 1.2")
            return bluetoothAdapter.getRemoteDevice(mac).createRfcommSocketToServiceRecord(uuid)
        }

        @SuppressLint("MissingPermission")
        fun connectToSocket(socket: BluetoothSocket){
            Debugger.printDebug("ASYNC NOW 2")
            try{
                socket.connect()
            }catch (e: IOException) {
                Debugger.printDebug("socket.connect() ERROR: read failed, socket might closed or timeout")
            }
            Debugger.printDebug("ASYNC NOW 3")
        }
        //manageMyConnectedSocket(bluetoothSocket)

        override fun run() {
            initSocket()
            connectToSocket(bluetoothSocket)
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    //Log.d(TAG, "Input stream was disconnected", e)
                    Debugger.printDebug("Input stream was disconnected IOException in Thread.run() - Thread Closed")
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, mmBuffer)
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                bluetoothSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}