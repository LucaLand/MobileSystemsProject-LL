package it.unibo.mobilesystems

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import it.unibo.mobilesystems.bluetoothUtils.DeviceInfoIntentResult
import it.unibo.mobilesystems.bluetoothUtils.MyBluetoothManager
import it.unibo.mobilesystems.bluetoothUtils.MyBluetoothService
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.ConfigManager
import it.unibo.mobilesystems.receivers.ActionHandler
import java.util.*

class BluetoothConncectionActivity : AppCompatActivity() {

    lateinit var startButton : Button
    lateinit var myBluetoothManager: MyBluetoothManager
    var resultIntent = Intent()

    var deviceName : String? = null
    var deviceAddress : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_conncection)

        /** UI ITEM - Init**/
        startButton = findViewById(R.id.StartButton)
        startButton.setOnClickListener { connectionPhaseDone() }

        //INIT CONFIG
        ConfigManager.init(this)

        val uuid = UUID.fromString(ConfigManager.getConfigString(UUID_CONFIG))
        deviceName = ConfigManager.getConfigString(ROBOT_DEVICE_NAME)
        deviceAddress = ConfigManager.getConfigString(ROBOT_DEVICE_ADDRESS)

        //TODO(RECEIVER FOR ROBOT FOUNDED and Connection Estabilished - We can use Handler)
        //RECEIVER FOR ROBOT FOUNDED (Action sended by MyBluetoothManager)
        registerReceiver(
            ActionHandler(ROBOT_FOUND_ACTION
            ) { context, intent ->
                createDeviceTextView("")
                if (intent != null) {
                    resultIntent = intent
                }
                MyBluetoothService.setDevice(intent?.getStringExtra(RESULT_DEVICE_ADDRESS_CODE)!!, UUID.fromString(intent.getStringExtra(RESULT_DEVICE_UUID_CODE)))
                MyBluetoothService.startSocketConnection()
            }.createBroadcastReceiver(), IntentFilter(ROBOT_FOUND_ACTION)
        )

        //SOCKET OPENED AND STABLE
        registerReceiver(ActionHandler(SOCKET_OPENED_ACTION){context, intent ->
            this.correctelyConnected()
            Debugger.printDebug("Bluetooth-Activity", "SOCKET_OPENED_ACTION arrived")
        }.createBroadcastReceiver(), IntentFilter(SOCKET_OPENED_ACTION))

        //Bluetooth Class Init
        myBluetoothManager = MyBluetoothManager(this)
        myBluetoothManager.bluetoothEnable()?.let { requestBluettothEnable(it) }
        MyBluetoothService.setServiceBluetoothAdapter(myBluetoothManager.bluetoothAdapter)

        var device : BluetoothDevice? = null
        if(uuid != null) {
            if (deviceName != null) {
                Debugger.printDebug("BLUETOOTH ACTIVITY", "Trying connection with DeviceName")
                device = myBluetoothManager.connectToDevice(deviceName!!, uuid)
            }else if(deviceAddress != null) {
                device = myBluetoothManager.connectToDevice(deviceAddress!!, uuid)
                Debugger.printDebug("BLUETOOTH ACTIVITY", "Trying connection with Address")
            }else
                Debugger.printDebug("No CONFIGURATION FOR DEVICE (Name or Address! in file.conf)")
        }else{
            Debugger.printDebug("ERROR - NO UUID Initialized from the file.conf")
        }
        if(device != null){
            Debugger.printDebug("BLUETOOTH ACTIVITY", "Device is not null")
            MyBluetoothService.setDevice(device.address, uuid)
            MyBluetoothService.startSocketConnection()
        }
    }

    private fun setActivityResult(deviceName: String, macAddress: String, uuid: String){
        resultIntent = DeviceInfoIntentResult.createIntentResult(deviceName, macAddress, uuid)
    }


    /**
     * UI FUNCTION
     * **/
    private fun createDeviceTextView(s : String) : TextView {
        val view: View = LayoutInflater.from(this).inflate(R.layout.device_text_view, null)
        (view as TextView).text = s
        return view
    }

    fun requestBluettothEnable(intent: Intent){
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Debugger.printDebug("requestBluettothEnable", "Accepted - Enabled")
                // Handle the Intent
            }
        }
        startForResult.launch(intent)
    }

    fun correctelyConnected(){
        startButton.isEnabled = true
    }


    /**
     * ON CLICK Functions
     * **/
    private fun connectionPhaseDone(){
        MyBluetoothService.sendMsg("START- Are you ready gitRobot?")
        this.finish()
    }

}