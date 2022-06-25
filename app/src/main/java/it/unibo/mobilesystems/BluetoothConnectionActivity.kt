package it.unibo.mobilesystems

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import it.unibo.mobilesystems.bluetooth.DEVICE_RESULT_CODE
import it.unibo.mobilesystems.bluetooth.DeviceInfoIntentResult
import it.unibo.mobilesystems.bluetooth.MyBluetoothManager
import it.unibo.mobilesystems.bluetooth.MyBluetoothService
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.ConfigManager
import it.unibo.mobilesystems.receivers.ActionHandler
import java.util.*

class BluetoothConnectionActivity : AppCompatActivity() {

    lateinit var startButton : Button
    lateinit var progressBar: ProgressBar

    lateinit var myBluetoothManager: MyBluetoothManager
    var resultIntent = Intent()

    var device : BluetoothDevice? = null
    var deviceName : String? = null
    var deviceAddress : String? = null
    var uuid : UUID? = null



    var researchDevice = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_conncection)

        /** UI ITEM - Init**/
        startButton = findViewById(R.id.StartButton)
        progressBar = findViewById(R.id.progressBar2)
        startButton.setOnClickListener { connectionPhaseDone() }

        //INIT CONFIG
        ConfigManager.init(this)

        uuid = UUID.fromString(ConfigManager.getConfigString(UUID_CONFIG))
        deviceName = ConfigManager.getConfigString(ROBOT_DEVICE_NAME)
        deviceAddress = ConfigManager.getConfigString(ROBOT_DEVICE_ADDRESS)

        //RECEIVER FOR ROBOT FOUNDED (Action sended by MyBluetoothManager)
        registerReceiver(
            ActionHandler(ROBOT_FOUND_ACTION
            ) { context, intent ->
                device = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                myBluetoothManager.bluetoothAdapter.cancelDiscovery()
                researchDevice = false
                findViewById<LinearLayout>(R.id.deviceBox).addView(createDeviceTextView("DEVICE: $deviceName"))
                MyBluetoothService.setDevice(intent?.getStringExtra(RESULT_DEVICE_ADDRESS_CODE)!!, UUID.fromString(intent.getStringExtra(RESULT_DEVICE_UUID_CODE)))
                MyBluetoothService.startSocketConnection()
            }.createBroadcastReceiver(), IntentFilter(ROBOT_FOUND_ACTION)
        )
        //RECEIVER FOR BLUETOOTH ACTIONS (START DISCOVERY; FINISHED DISCOVERY; BLUETOOTH STATE CHANGED
        registerReceiver(ActionHandler(BluetoothAdapter.ACTION_DISCOVERY_STARTED) { context, intent ->
            this.searchStart()
            researchDevice = true }.createBroadcastReceiver(), IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(ActionHandler(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {context, intent -> this.searchFinished() }.createBroadcastReceiver(), IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        registerReceiver(ActionHandler(BluetoothAdapter.ACTION_STATE_CHANGED) {context, intent ->
            myBluetoothManager.bluetoothEnable()?.let { requestBluettothEnable(it) }
        }.createBroadcastReceiver(), IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))


        //SOCKET OPENED AND STABLE
        registerReceiver(ActionHandler(SOCKET_OPENED_ACTION){context, intent ->
            this.correctelyConnected()
            Debugger.printDebug("Bluetooth-Activity", "SOCKET_OPENED_ACTION arrived")
            //findViewById<LinearLayout>(R.id.deviceBox).addView(createDeviceTextView("DEVICE: ${deviceName}"))
        }.createBroadcastReceiver(), IntentFilter(SOCKET_OPENED_ACTION))

        //SOCKET ERROR AND CLOSED
        registerReceiver(ActionHandler(SOCKET_CLOSED_ACTION){ context, intent ->
            this.connectionError()
            Debugger.printDebug("Bluetooth-Activity", "SOCKET_CLOSED_ACTION arrived")
        }.createBroadcastReceiver(), IntentFilter(SOCKET_CLOSED_ACTION))

        //Bluetooth Class Init
        myBluetoothManager = MyBluetoothManager(this)
        myBluetoothManager.bluetoothEnable()?.let { requestBluettothEnable(it) }
        MyBluetoothService.setServiceBluetoothAdapter(myBluetoothManager.bluetoothAdapter)

        if(uuid != null) {
            if (deviceName != null) {
                Debugger.printDebug("BLUETOOTH ACTIVITY", "Trying connection with DeviceName")
                device = myBluetoothManager.connectToDevice(deviceName!!, uuid!!)
            }else
                Debugger.printDebug("No CONFIGURATION FOR DEVICE (Name or Address! in file.conf)")

            if(device != null){
                Debugger.printDebug("BLUETOOTH ACTIVITY", "Device is Already Paired!")
                findViewById<LinearLayout>(R.id.deviceBox).addView(createDeviceTextView("DEVICE: ${deviceName}"))
                MyBluetoothService.setDevice(device!!.address, uuid!!)
                MyBluetoothService.startSocketConnection()
            }else{
                Debugger.printDebug("BLUETOOTH ACTIVITY", "Device not Paired!")
            }
        }else{
            Debugger.printDebug("ERROR - NO UUID Initialized from the file.conf")
        }

    }

    private fun searchStart() {
        progressBar.animate()
    }

    private fun searchFinished() {
        if(researchDevice) {
            if (deviceName != null && uuid != null) {
                myBluetoothManager.connectToDevice(this.deviceName!!, this.uuid!!)
            }
        }else
            progressBar.isVisible = false
    }

    private fun connectionError() {
        startButton.isEnabled = false
        progressBar.isVisible = true
    }

    private fun setActivityResult(){
        if(device != null) {
            resultIntent =
                DeviceInfoIntentResult.createIntentResult(device?.name!!, device?.address!!, uuid.toString())
            resultIntent.putExtra(DEVICE_RESULT_CODE, device)
        }
    }


    /**
     * UI FUNCTION
     * **/
    private fun createDeviceTextView(s : String) : TextView {
        val view: View = LayoutInflater.from(this).inflate(R.layout.device_text_view, null)
        (view as TextView).text = s
        return view
    }

    private fun requestBluettothEnable(intent: Intent){
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Debugger.printDebug("requestBluetoothEnable", "Accepted - Enabled")
                // Handle the Intent
            }
        }
        startForResult.launch(intent)
    }

    fun correctelyConnected(){
        startButton.isEnabled = true
        progressBar.isVisible = false
    }


    /**
     * ON CLICK Functions
     * **/
    private fun connectionPhaseDone(){
        //MyBluetoothService.sendMsg("START- Are you ready gitRobot?")
        setActivityResult()
        setResult(RESULT_OK, resultIntent)
        this.finish()
    }

}