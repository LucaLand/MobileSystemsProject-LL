package it.unibo.mobilesystems

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import it.unibo.kactor.annotations.QActor
import it.unibo.kactor.annotations.StartMode
import it.unibo.kactor.model.TransientStartMode
import it.unibo.mobilesystems.actors.GIT_BERTO_CTX_NAME
import it.unibo.mobilesystems.actors.qakBluetoothConnection
import it.unibo.mobilesystems.bluetooth.*
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.ConfigManager
import it.unibo.mobilesystems.permissionManager.PermissionType
import it.unibo.mobilesystems.permissionManager.PermissionsManager
import it.unibo.mobilesystems.utils.OkDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class BluetoothConnectionActivity : AppCompatActivity() {

    companion object {
        val ACTIVITY_NAME = "BLUETOOTH_ACTIVITY"
    }

    lateinit var startButton : Button
    lateinit var progressBar: ProgressBar

    private lateinit var bluetoothController : BluetoothController
    var resultIntent = Intent()

    var device : BluetoothDevice? = null
    var deviceName : String? = null
    var deviceAddress : String? = null
    var uuidString : String? = null
    var uuid : UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_conncection)

        bluetoothController = BluetoothController(this)

        /** UI ITEM - Init**/
        startButton = findViewById(R.id.StartButton)
        progressBar = findViewById(R.id.progressBar2)
        startButton.setOnClickListener { connectionPhaseDone() }

        //INIT CONFIG
        ConfigManager.init(this)

        uuidString= ConfigManager.getConfigString(UUID_CONFIG)
        checkUUIDPresentOrClose()
        uuid = UUID.fromString(uuidString)
        deviceName = ConfigManager.getConfigString(ROBOT_DEVICE_NAME)
        deviceAddress = ConfigManager.getConfigString(ROBOT_DEVICE_ADDRESS)

        Debugger.printDebug(ACTIVITY_NAME, "setting up bluetooth")
        trySetupBluetoothOrClose{ setupResult ->
            Debugger.printDebug(ACTIVITY_NAME, "bluetooth setup result: $setupResult")
            if(setupResult.resultCode == Activity.RESULT_CANCELED) {
                Debugger.printDebug(ACTIVITY_NAME, "setup bluetooth failed")
                finishAffinity()
            }

            Debugger.printDebug(ACTIVITY_NAME, "bluetooth setup completed")
            setupConnection()   //Search first into already paired device
            //then, if no device is found, start a bluetooth discovery.
            //All is done asynchronously
            Toast.makeText(applicationContext,
                "Searching GitBerto via Bluetooth",
                LENGTH_SHORT
            )
            Debugger.printDebug(ACTIVITY_NAME, "started connection mechanism")
        }
    }

    private fun trySetupBluetoothOrClose(onBluetoothSetup : (ActivityResult) -> Unit) {
        try {
            PermissionsManager.permissionCheck(PermissionType.Bluetooth, this)
            bluetoothController.asyncSetupBluetooth(onBluetoothSetup)
        } catch (e : Exception) {
            OkDialogFragment("Unable to setup bluetooth: ${e.localizedMessage}") {
                e.printStackTrace()
                Debugger.printDebug(ACTIVITY_NAME, "setup bluetooth failed due to exception: ${e.localizedMessage}")
                finishAffinity()
            }.show(supportFragmentManager, OkDialogFragment.TAG)
        }
    }

    private fun checkUUIDPresentOrClose() {
        if(uuidString == null) {
            OkDialogFragment("Unable to search for QAK service: missing UUID") {
                Debugger.printDebug(ACTIVITY_NAME, "unable to get the UUID of the QAK service")
                finishAffinity()
            }.show(supportFragmentManager, OkDialogFragment.TAG)
        }
    }

    private fun setupConnection() {
        progressBar.animate()
        //Search for device
        val devices = bluetoothController.getPairedDevicesOfferingService(uuidString!!)
        Debugger.printDebug(ACTIVITY_NAME, "found previously paired device: $device")
        pairedDeviceConnectIterationOrSearchForDevices(devices.iterator()) //call searchForDevice when finished
    }

    private fun pairedDeviceConnectIterationOrSearchForDevices(iterator : Iterator<BluetoothDevice>) {
        if(iterator.hasNext()) {
            val device = iterator.next()
            Debugger.printDebug(ACTIVITY_NAME, "paired device iteration [device: $device]")
            bluetoothController.asyncConnect(device, uuidString!!) { connectResult ->
                Debugger.printDebug(ACTIVITY_NAME, "connection result: $connectResult [device: $device]")
                if(connectResult.isSuccess) { //Device connected
                    onConnectionSucceeded(device, connectResult.getOrThrow())
                } else { //Device not connected
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext,
                            "connection failed with paired device [${device.address}:${device.name}]: ${
                                connectResult.exceptionOrNull()?.localizedMessage}",
                            LENGTH_SHORT
                        )
                    }
                    pairedDeviceConnectIterationOrSearchForDevices(iterator)
                }
            }
        } else { //Finished iteration
            searchForDevice()
        }
    }

    private fun searchForDevice() {
        Debugger.printDebug("searching for available bluetooth devices")

        bluetoothController.asyncDiscoverySearch {
            findFirstThatOffersService(uuidString!!)
            whenFound { device ->
                Debugger.printDebug(ACTIVITY_NAME, "found compatible device: $device")
                bluetoothController.asyncConnect(device, uuidString!!) { connectResult ->
                    Debugger.printDebug(ACTIVITY_NAME, "connection result: $connectResult [device: $device]")
                    if(connectResult.isSuccess) {
                        onConnectionSucceeded(device, connectResult.getOrThrow())
                    } else {
                        continueDiscoveryAfterFound()
                        Toast.makeText(applicationContext,
                            "connection failed with found device [${device.address}:${device.name}]: ${
                                connectResult.exceptionOrNull()?.localizedMessage}",
                            LENGTH_SHORT
                        )
                    }
                }
            }
        }
    }

    //Executed asynchronously when connection succeeded
    private suspend fun onConnectionSucceeded(device : BluetoothDevice, socket : BluetoothSocket) {
        Debugger.printDebug(ACTIVITY_NAME, "onConnectionSucceeded [device=$device]")
        withContext(Dispatchers.Main) {
            startButton.isEnabled = true //TODO: automatize
            progressBar.isVisible = false
            Toast.makeText(applicationContext,
                "connection succeeded [${device.address}:${device.name}]",
                LENGTH_SHORT
            )
        }
        qakBluetoothConnection.set(socket.qakConnection("git-berto-main-conn"))
    }

    private fun setActivityResult(){
        if(device != null) {
            resultIntent =
                DeviceInfoIntentResult.createIntentResult(device?.name!!, device?.address!!, uuid.toString())
            resultIntent.putExtra(DEVICE_RESULT_CODE, device)
        }
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