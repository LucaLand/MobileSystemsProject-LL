package it.unibo.mobilesystems.bluetoothUtils


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.unibo.mobilesystems.BluetoothActivity
import it.unibo.mobilesystems.R
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.debugUtils.DebuggerContextNameAnnotation
import it.unibo.mobilesystems.fileUtils.FileSupport
import java.util.*


class BluetoothTest : BluetoothActivity() {

    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter

    lateinit var bluetoothBtn : FloatingActionButton



     var bluetoothDevicesDiscovered = mutableListOf<BluetoothDevice?>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothBtn = findViewById(R.id.bluetoothButton)
        //LISTENER
        bluetoothBtn.setOnClickListener{refreshBluetoothPage()}
        longClickListener = View.OnLongClickListener{ view : View -> onDeviceClick(view)}

        addReceivers(this)

        bluetoothInit()
        bluetoothEnable()
        refreshBluetoothPage()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiverBluetoothDevices)
        unregisterReceiver(receiverScanStart)
        unregisterReceiver(receiverScanEnd)
        unregisterReceiver(receiverBluetoothStateChanged)
    }

    fun bluetoothInit() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        this.bluetoothManager = bluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    @SuppressLint("MissingPermission")
    fun bluetoothEnable(){
        if (!bluetoothAdapter.isEnabled) {
            debugger.printDebug("Bluetooth State: DISBALED")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 4)
        }else{
            debugger.printDebug("Bluetooth State: ENABLED")
            bluetoothAdapter.enable()
        }
    }

    fun refreshBluetoothPage(){
        pageClean()
        printBluetoothPairedDevices()
        bluetoothSearch()
    }

    @SuppressLint("MissingPermission")
    fun printBluetoothPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        printPairedDevices(pairedDevices)
    }

    @SuppressLint("MissingPermission")
    fun bluetoothSearch(){
        bluetoothAdapter.cancelDiscovery()
        debugger.printDebug(bluetoothAdapter.startDiscovery())
    }

    /** RECEIVERS*/

    private fun addReceivers(app : AppCompatActivity){
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        app.registerReceiver(receiverBluetoothDevices, filter)
        //debugger.printDebug("Registered Broadcast ReciverFound : $receiverBluetoothDevices")

        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        app.registerReceiver(receiverScanStart, filter)
        //debugger.printDebug("Registered Broadcast ReciverStart : $receiverScanStart")

        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        app.registerReceiver(receiverScanEnd, filter)
        //debugger.printDebug("Registered Broadcast ReciverEnd : $receiverScanStart")

        filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        app.registerReceiver(receiverBluetoothStateChanged, filter)
        //debugger.printDebug("Registered Broadcast ReciverEnd : $receiverScanStart")
    }

    private val receiverBluetoothDevices = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if(device != null && !bluetoothDevicesDiscovered.contains(device)) bluetoothDevicesDiscovered.add(device)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                    //printDevice("Nome: $deviceName - MAC: $deviceHardwareAddress")
                    //Debug Log
                    debugger.printDebug("Nome: $deviceName - MAC: $deviceHardwareAddress")
                }
            }
        }
    }


    private val receiverScanStart = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    debugger.printDebug("DISCOVERY - START")
                    loadingBar.isGone = false
                }
            }
        }
    }

    private val receiverScanEnd = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    debugger.printDebug("DISCOVERY - END")
                    printDiscoveredDevices(bluetoothDevicesDiscovered)
                    loadingBar.isGone = true
                }
            }
        }
    }

    private val receiverBluetoothStateChanged = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if(bluetoothAdapter.isEnabled) {
                        debugger.printDebug("Bluetooth State Changed: Enabled")
                        bluetoothAdapter.enable()
                        refreshBluetoothPage()
                    }else{
                        debugger.printDebug("Bluetooth State Changed: Disabled")
                        bluetoothEnable()
                    }
                }
            }
        }
    }


    /** LISTENER **/
    fun onDeviceClick(view : View) : Boolean{
        var txt = (view as TextView).text
        var strings = txt.split(" || ")
        var deviceName = strings[0].split("Nome: ")[1].trim()
        var deviceMac = strings[1].split("MAC: ")[1].trim()
        debugger.printDebug(strings[1].split("MAC: "))
        debugger.printDebug("DEVICE SELECTED: $deviceName - $deviceMac")
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
        view.setBackgroundColor(Color.rgb(70,70,70))
        //deviceConnect(deviceMac)
        return true
    }

    private fun deviceConnect(mac : String) {
        TODO("Not yet implemented")
        //bluetoothAdapter.getRemoteDevice(mac)
    }


}