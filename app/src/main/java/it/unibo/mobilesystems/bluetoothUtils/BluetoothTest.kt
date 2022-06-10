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

private const val FILE_NAME = "file.conf"

class BluetoothTest : BluetoothActivity() {

    private lateinit var bluetoothBtn : FloatingActionButton

    var uuid : UUID? = null
    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter

    var bluetoothDevicesDiscovered = mutableListOf<BluetoothDevice?>()

    lateinit var myBluetoothService : MyBluetoothService
    lateinit var bluetoothThread : MyBluetoothService.BluetoothSocketThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothBtn = findViewById(R.id.bluetoothButton)
        //LISTENER
        bluetoothBtn.setOnClickListener{refreshBluetoothPage()}
        longClickListener = View.OnLongClickListener{ view : View -> onDeviceClick(view)}
        //Add Receivers for Bluetooth Actions
        addReceivers(this)

        //Bluetooth Initialization
        bluetoothInit()
        bluetoothEnable()
        refreshBluetoothPage()

        uiidInit()
        Debugger.printDebug("UUID: $uuid")
        myBluetoothService = MyBluetoothService(Handler.createAsync(Looper.myLooper()!!))
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiverBluetoothDevices)
        unregisterReceiver(receiverScanStart)
        unregisterReceiver(receiverScanEnd)
        unregisterReceiver(receiverBluetoothStateChanged)
        bluetoothAdapter.cancelDiscovery()
    }

    @SuppressLint("MissingPermission")
    override fun onStop() {
        super.onStop()
        bluetoothAdapter.cancelDiscovery()
    }

    private fun uiidInit(){
        uuid = FileSupport.getUUIDFromAssetFile(FILE_NAME, this)
        if(uuid == null)
            uuid = UUID.randomUUID()
    }

    /**
     * ----------BLUETOOTH INIT & SEARCH FUNCTION------------
     * **/
    private fun bluetoothInit() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        this.bluetoothManager = bluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    @SuppressLint("MissingPermission")
    fun bluetoothEnable(){
        if (!bluetoothAdapter.isEnabled) {
            Debugger.printDebug("Bluetooth State: DISABLED")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 4)
        }else{
            Debugger.printDebug("Bluetooth State: ENABLED")
            bluetoothAdapter.enable()
        }
    }

    @SuppressLint("MissingPermission")
    fun bluetoothSearch(){
        bluetoothAdapter.cancelDiscovery()
        Debugger.printDebug(bluetoothAdapter.startDiscovery())
    }


    /**
     * ----------------- BLUETOOTH SOCKET FUNCTION --------------------
     * **/
    @SuppressLint("MissingPermission")
    private fun deviceConnect(mac : String) {
        bluetoothAdapter.cancelDiscovery()
        Debugger.printDebug("ASYNC NOW 1")
        bluetoothThread = myBluetoothService.BluetoothSocketThread(bluetoothAdapter, mac, uuid!!)
        bluetoothThread.start()
        Debugger.printDebug("ASYNC DONE 4")
    }

    private fun bluetoothSendData(th : MyBluetoothService.BluetoothSocketThread, s: String){
        th.write(s.toByteArray())
    }



    /**
     * ----------Page FUNCTION------------
     * **/
    fun refreshBluetoothPage(){
        pageClean()
        printBluetoothPairedDevices()
        bluetoothSearch()
        enableLoadingBar(true)
        bluetoothDevicesDiscovered.clear()
    }

    @SuppressLint("MissingPermission")
    fun printBluetoothPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        printPairedDevices(pairedDevices)
    }

    fun enableLoadingBar(boolean: Boolean){
        loadingBar.isVisible = boolean
    }



    /**
     * ---------- RECEIVER ------------
     * **/
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

    //FINDING DEVICES
    private val receiverBluetoothDevices = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @DebuggerContextNameAnnotation("DISCOVERY")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if(device?.name!= null && !bluetoothDevicesDiscovered.contains(device)) {
                        bluetoothDevicesDiscovered.add(device)
                        val deviceString = deviceToString(device)
                        printDiscoveredDevice(deviceString)
                        Debugger.printDebug(deviceString)
                    }
                }
            }
        }
    }

    //SCAN START
    private val receiverScanStart = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Debugger.printDebug("DISCOVERY - START")
                    enableLoadingBar(true)
                }
            }
        }
    }

    //SCAN END
    private val receiverScanEnd = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Debugger.printDebug("DISCOVERY - END")
                    enableLoadingBar(false)
                    //printDiscoveredDevices(bluetoothDevicesDiscovered)
                }
            }
        }
    }

    //BLUETOOTH STATE CHANGED
    private val receiverBluetoothStateChanged = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if(bluetoothAdapter.isEnabled) {
                        Debugger.printDebug("Bluetooth State Changed: Enabled")
                        bluetoothAdapter.enable()
                        refreshBluetoothPage()
                    }else{
                        Debugger.printDebug("Bluetooth State Changed: Disabled")
                        bluetoothEnable()
                    }
                }
            }
        }
    }

    /**
     * ------------- LISTENER -----------------
     * **/
    private fun onDeviceClick(view : View) : Boolean{
        val txt = (view as TextView).text
        val strings = txt.split(" || ")
        val deviceName = strings[0].split("Nome: ")[1].trim()
        val deviceMac = strings[1].split("MAC: ")[1].trim()
        Debugger.printDebug(strings[1].split("MAC: "))
        Debugger.printDebug("DEVICE SELECTED: $deviceName - $deviceMac")
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
        view.setBackgroundColor(Color.rgb(70,70,70))
        deviceConnect(deviceMac)
        return true
    }




}