package it.unibo.mobilesystems

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import it.unibo.mobilesystems.bluetoothUtils.MyBluetoothManager
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.ConfigManager
import java.util.*

class BluetoothConncectionActivity : AppCompatActivity() {

    lateinit var startButton : Button
    lateinit var myBluetoothManager: MyBluetoothManager

    //TEST
    var deviceName : String? = null
    var deviceAddress : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_conncection)

        /** UI ITEM - Init**/
        startButton = findViewById(R.id.StartButton)
        startButton.setOnClickListener { connectionFinished() }

        ConfigManager.init(this)
        //to pass from the main activity
        val uuid = UUID.fromString(ConfigManager.getConfigString(UUID_CONFIG))
        deviceName = ConfigManager.getConfigString(ROBOT_DEVICE_NAME)
        deviceAddress = ConfigManager.getConfigString(ROBOT_DEVICE_ADDRESS)

        //TODO(RECEIVER FOR ROBOT FOUNDED and Connection Estabilished - We can use Handler)
        //RECEIVER FOR ROBOT FOUNDED (Action sended by MyBluetoothManager)
        /*
        registerReceiver(
            ActionHandler(ROBOT_FOUND_ACTION
            ) { context, intent ->
                createDeviceTextView()
            }.createBroadcastReciver(),IntentFilter(ROBOT_FOUND_ACTION))

         */

        //Bluetooth Class Init
        myBluetoothManager = MyBluetoothManager(this)
        myBluetoothManager.bluetoothEnable()?.let { requestBluettothEnable(it) }


        if(uuid != null) {
            if (deviceName != null)
                myBluetoothManager.connectToDevice(deviceName!!, uuid)
            else if(deviceAddress != null)
                myBluetoothManager.connectToDevice(deviceAddress!!, uuid)
            else
                Debugger.printDebug("No CONFIGURATION FOR DEVICE (Name or Address! in file.conf)")
        }else{
            Debugger.printDebug("ERROR - NO UUID Initialized from the file.conf")
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

    fun requestBluettothEnable(intent: Intent){
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Debugger.printDebug("requestBluettothEnable", "Accepted - Enabled")
                // Handle the Intent
            }
        }
        startForResult.launch(intent)
    }


    /**
     * ON CLICK Functions
     * **/
    private fun connectionFinished(){
        finishActivity(BLUETOOTH_CONNECT_ACTIVITY_CODE)
    }

}