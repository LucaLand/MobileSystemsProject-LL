package it.unibo.mobilesystems

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.core.view.isVisible
import it.unibo.kactor.IQActorBasic.*
import it.unibo.kactor.IQActorBasicFsm
import it.unibo.kactor.annotations.*
import it.unibo.kactor.model.TransientStartMode
import it.unibo.kactor.qakActorFsm
import it.unibo.mobilesystems.actors.*
import it.unibo.mobilesystems.bluetooth.*
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.fileUtils.ConfigManager
import it.unibo.mobilesystems.utils.AdapterDialog
import it.unibo.mobilesystems.utils.OkDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import kotlin.jvm.optionals.getOrNull

private const val SETUP_BT_CMD = "setupBluetooth"
private const val BEGIN_CONNECTION_CMD = "beginConnection"

@QActor(GIT_BERTO_CTX_NAME)
@StartMode(TransientStartMode.MANUAL)
class BluetoothConnectionActivity : ActorAppCompactActivity(),
    IQActorBasicFsm by qakActorFsm(BluetoothConnectionActivity::class.java, Dispatchers.Default, DEFAULT_PARAMS) {

    companion object {
        val ACTIVITY_NAME = "BLUETOOTH_ACTIVITY"
    }

    lateinit var startButton : Button
    lateinit var progressBar: ProgressBar
    lateinit var mainTextView : TextView

    private lateinit var bluetoothController : BluetoothController
    var resultIntent = Intent()

    var device : BluetoothDevice? = null
    var deviceName : String? = null
    var deviceAddress : String? = null
    var uuidString : String? = null
    var uuid : UUID? = null

    val channel = Channel<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Debugger.printDebug(ACTIVITY_NAME, "onCreate")
        setContentView(R.layout.activity_bluetooth_conncection)

        bluetoothController = BluetoothController(this)

        /** UI ITEM - Init**/
        mainTextView = findViewById(R.id.mainTextView)
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
        Debugger.printDebug(ACTIVITY_NAME, "bluetooth supported: ${bluetoothController.isBluetoothSupported}")

        bluetoothController.setupBluetooth {
            lifecycleLaunch {
                autoMsg(SETUP_BT_CMD, "setup")
                Debugger.printDebug(ACTIVITY_NAME, "autoMsg: $SETUP_BT_CMD")
            }
        }
        Debugger.printDebug(ACTIVITY_NAME, "onCreate - finished")
    }

    /* ACTOR PART *********************************************************************** */
    private var connectedDevice : BluetoothDevice? = null
    private var connectedSocket : BluetoothSocket? = null
    private var stringifier : (BluetoothDevice) -> String = {
        "${it.name} [${it.address}]"
    }

    private lateinit var adapterDialog : AdapterDialog<BluetoothDevice>

    @OptIn(ExperimentalStdlibApi::class)
    @State
    @Initial
    @WhenDispatch("idle2setupBluetooth", "setupBluetooth", SETUP_BT_CMD)
    suspend fun begin() {
        Debugger.printDebug(name, actorStringln("-> BEGIN"))
        if(deviceAddress != null)
            device = bluetoothController.getPairedDeviceByAddress(deviceAddress!!).getOrNull()
        updateUi {
            adapterDialog = AdapterDialog(this, "Scanned Devices",
                android.R.layout.simple_list_item_1, stringifier)
            adapterDialog.androidDialog.setCancelable(false)
            adapterDialog.androidDialog.setCanceledOnTouchOutside(false)
        }
        Debugger.printDebug(name, actorStringln("dialog for discovery set"))
    }

    @State
    @EpsilonMove("afterSetup", "connectToSavedDevice")
    suspend fun setupBluetooth() {
        Debugger.printDebug(name, actorStringln("-> SETUP BLUETOOTH"))
        updateUi {
            progressBar.animate()
        }
    }

    @GuardFor("afterSetup", "unableToSetup")
    fun checkBluetooth() : Boolean {
        return bluetoothController.isBluetoothSetup
    }

    @State
    suspend fun unableToSetup() {
        Debugger.printDebug(name, actorStringln("-> UNABLE TO SETUP"))
        updateUi {
            mainTextView.text = "Unable to setup bluetooth"
        }
    }

    @State
    @EpsilonMove("afterConnectionToSaved", "connected") //GUARDED, else: scenForNewOnes
    suspend fun connectToSavedDevice() {
        Debugger.printDebug(name, actorStringln("-> BEGIN CONNECTION [address: $deviceAddress, uuid: $uuidString"))

        updateUi { if(!progressBar.isAnimating) progressBar.animate() }

        //Attempt to connect to paired device
        if(device != null && uuidString != null) {
            Debugger.printDebug(name, "trying to connect with the last device...")
            try {
                connectedSocket = bluetoothController.connectToRfcommService(device!!, uuidString!!)
                connectedDevice = device
            } catch (ioe : IOException) {
                Debugger.printDebug(name, actorStringln("connection with last device failed: ${ioe.localizedMessage}"))
                updateUi {
                    Toast.makeText(this,
                        "Unable to connect with GitBerto [$deviceName:$deviceAddress]",
                        Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Debugger.printDebug(name, actorStringln("no device previously associated"))
        }

    }

    @GuardFor("afterConnectionToSaved", "scanForNewOnes")
    fun isConnected() : Boolean {
        return connectedDevice != null
    }

    @State
    @EpsilonMove("scanForNewOnes2connectToSavedDevice", "connectToSavedDevice")
    suspend fun scanForNewOnes() {
        Debugger.printDebug(name, actorStringln("-> SCAN FOR NEW ONES [isDiscovering: ${bluetoothController.isDiscovering.get()}]"))
        var selectedDevice : BluetoothDevice? = null
        val scannedDevice = mutableSetOf<BluetoothDevice>()
        adapterDialog.clear()
        while(selectedDevice == null) {
            adapterDialog.addOnSelection {
                selectedDevice = it
                actorLaunch { bluetoothController.cancelDiscovery() }
            }
            updateUi {
                adapterDialog.show()
            }
            bluetoothController.discoverDevices {
                if(scannedDevice.add(it)) {
                    adapterDialog.addItem(it)
                }
            }
        }
        Debugger.printDebug(name, actorStringln("device selection done: $selectedDevice"))
        device = selectedDevice
        deviceAddress = selectedDevice!!.address
        deviceName = selectedDevice!!.name


        //Then selectedDevice is not null
        updateUi {
            Toast.makeText(this,
                "Trying to connect to ${selectedDevice!!.name}:${selectedDevice!!.address}",
                Toast.LENGTH_LONG
            )
        }
    }

    @State
    suspend fun connected() {
        updateUi { mainTextView.text = "CONNECTED: $device" }
        val gattCallback = LambdaGattCallback(APP_SCOPE)
        val gatt = connectedDevice!!.connectGatt(applicationContext, false, gattCallback)
        GattActor.gattDescriptor.set(GattDescriptor(gatt, gattCallback))
        send dispatch UPDATE_GATT_DESCRIPTOR_MSG_NAME to GATT_ACTOR_NAME withContent "update"
        delay(500)
        send dispatch DO_POLLING_MSG_NAME to GATT_ACTOR_NAME withContent "polling"
        onConnectionSucceeded(connectedDevice!!, connectedSocket!!)
    }

    private fun checkUUIDPresentOrClose() {
        if(uuidString == null) {
            OkDialogFragment("Unable to search for QAK service: missing UUID") {
                Debugger.printDebug(ACTIVITY_NAME, "unable to get the UUID of the QAK service")
                finishAffinity()
            }.show(supportFragmentManager, OkDialogFragment.TAG)
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