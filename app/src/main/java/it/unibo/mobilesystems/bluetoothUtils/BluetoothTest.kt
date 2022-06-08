package it.unibo.mobilesystems.bluetoothUtils


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.unibo.mobilesystems.BluetoothActivity
import it.unibo.mobilesystems.R
import it.unibo.mobilesystems.debugUtils.debugger


class BluetoothTest : BluetoothActivity() {

    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var bluetoothBtn : FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothBtn = findViewById(R.id.bluetoothButton)
        bluetoothBtn.setOnClickListener{bluetoothSearch()}

        addReceivers(this)

        bluetoothInit()
        bluetoothEnable()
        bluetoothSearch()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiverBluetoothDevices)
        unregisterReceiver(receiverScanStart)
        unregisterReceiver(receiverScanEnd)
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

    @SuppressLint("MissingPermission")
    fun bluetoothSearch(){
        bluetoothAdapter.cancelDiscovery()
        debugger.printDebug(bluetoothAdapter.startDiscovery())
    }

    fun addReceivers(app : AppCompatActivity){
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
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                    printDevice("Nome: $deviceName - MAC: $deviceHardwareAddress")

                    //Debug Log
                    debugger.printDebug("$device")
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
                    //bluetoothManager.
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
                        bluetoothSearch()
                    }else{
                        debugger.printDebug("Bluetooth State Changed: Disabled")
                        bluetoothEnable()
                    }
                }
            }
        }
    }
}