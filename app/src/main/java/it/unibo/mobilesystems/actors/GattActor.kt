package it.unibo.mobilesystems.actors

import android.bluetooth.BluetoothGatt
import com.google.gson.Gson
import it.unibo.kactor.QActorBasicFsm
import it.unibo.kactor.IQActorBasic.*
import it.unibo.kactor.annotations.*
import it.unibo.mobilesystems.bluetooth.GattDescriptor
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.utils.atomicNullableVar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val GATT_ACTOR_NAME = "gattactor"

const val DO_POLLING_MSG_NAME = "dopolling"
const val STOP_POLLING_MSG_NAME = "stoppolling"
const val READ_RSSI_REQ_NAME = "readrssi"
const val READ_RSSI_REPLY_NAME = "readrssireply"
const val UPDATE_GATT_DESCRIPTOR_MSG_NAME = "updategatt"
const val SET_POLLING_DELAY_MILLIS_MSG_NAME = "setpollingdelay"

const val GATT_STATUS_EVENT_NAME = "gattstatus"
const val RSSI_VALUE_EVENT_NAME = "rssivalue"

@QActor(GIT_BERTO_CTX_NAME, GATT_ACTOR_NAME)
class GattActor : QActorBasicFsm() {

    companion object {
        var gattDescriptor = atomicNullableVar<GattDescriptor>()
    }

    /* CALLBACK FOR GATT **************************************************************** */
    private val emitEventCallback : (gatt: BluetoothGatt,
                                     rssi: Int,
                                     status: Int) -> Unit = { _, rssi, _ ->
        actor.scope.launch {
            emit event RSSI_VALUE_EVENT_NAME withContent rssi.toString()
        }
    }

    private var currentRssi = 0
    private val setInternalRssiVarCallback : (gatt: BluetoothGatt,
                                     rssi: Int,
                                     status: Int) -> Unit = { _, rssi, _ ->
        currentRssi = rssi
    }

    private val emitEventOnStateChange : (gatt : BluetoothGatt?, state: Int, newStatus: Int) -> Unit = {
        _, _, newStatus ->
        actor.scope.launch {
            emit event GATT_STATUS_EVENT_NAME withContent newStatus.toString()
        }
    }
    /* ********************************************************************************** */

    private var isPolling = false
    private var pollingDelay = 1000L
    private var actorGattDescriptor : GattDescriptor? = null
    private val gson = Gson()

    @State
    @Initial
    @EpsilonMove("start2idle", "idle")
    suspend fun begin() {
        Debugger.printDebug(name, actorStringln("started"))
        updateGatt()
    }

    @State
    @WhenDispatch("idle2enablePolling", "enablePolling",
        DO_POLLING_MSG_NAME) //Guarded: if gatt null, return to idle
    @WhenRequest("idle2handleRssiRequest", "handleRssiRequest", READ_RSSI_REQ_NAME)
    @WhenDispatch("idle2updateGatt", "updateGatt", UPDATE_GATT_DESCRIPTOR_MSG_NAME)
    @WhenDispatch("idle2setPollingDelay", "setPollingDelay", SET_POLLING_DELAY_MILLIS_MSG_NAME)
    suspend fun idle() {
        Debugger.printDebug(name, actorStringln("idle"))
    }

    @State
    @EpsilonMove("enablePolling2polling", "polling")
    suspend fun enablePolling() {
        isPolling = true
        Debugger.printDebug("polling enabled")
    }

    @State
    @EpsilonMove("disablePolling2idle", "idle")
    suspend fun disablePolling() {
        isPolling = false
        Debugger.printDebug("polling disabled")
    }

    @State
    @EpsilonMove("pollAgain", "polling")
    @WhenDispatch("polling2disablePolling", "disablePolling", STOP_POLLING_MSG_NAME)
    @WhenDispatch("polling2setPollingDelay", "setPollingDelay", SET_POLLING_DELAY_MILLIS_MSG_NAME)
    @WhenRequest("polling2handleRssiRequest", "handleRssiRequest", READ_RSSI_REQ_NAME)
    suspend fun polling() {
        Debugger.printDebug(name, actorStringln("polling"))
        actorGattDescriptor!!.gatt.readRemoteRssi() //Automatically emit the event
                                                    //and set the currentRssi var
        delay(pollingDelay)
    }

    @State
    @EpsilonMove("setPollingDelay2polling", "polling") //GUARDED: return to idle if
                                                                            //is not polling
    suspend fun setPollingDelay() {
        try {
            pollingDelay = currentMsg.msgContent().toLong()
            Debugger.printDebug(name, actorStringln("set polling delay: $pollingDelay millis"))
        } catch (_ : NumberFormatException) {
            Debugger.printDebug(name, actorStringln("unable to parse polling delay from message \'$currentMsg\'"))
        }
    }

    @State
    @EpsilonMove("afterReadRssi", "polling")//Guarded
    suspend fun handleRssiRequest() {
        if(actorGattDescriptor == null) {
            replyTo request READ_RSSI_REQ_NAME with READ_RSSI_REPLY_NAME withContent gson
                .toJson(Result.failure<Int>(NullPointerException("gat is null")))
            Debugger.printDebug(name, actorStringln("unable to get rssi value: gatt is null"))
        } else {
            actorGattDescriptor!!.gatt.readRemoteRssi() //Automatically set the currentRssi var
            replyTo request READ_RSSI_REQ_NAME with READ_RSSI_REPLY_NAME withContent gson
                .toJson(Result.success(currentRssi))
            Debugger.printDebug(name, actorStringln("replied with value $currentRssi"))
        }
    }

    @GuardFor("afterReadRssi", "idle")
    @GuardFor("idle2enablePolling", "idle")
    @GuardFor("afterUpdateGatt", "idle")
    @GuardFor("setPollingDelay2polling", "idle")
    @GuardFor("polling2setPollingDelay", "idle")
    fun ifIsPolling() : Boolean {
        if(actorGattDescriptor == null) {
            Debugger.printDebug(name, actorStringln("gatt descriptor is null, returning to idle..."))
            isPolling = false
            return false
        }
        return isPolling
    }

    @State
    @EpsilonMove("afterUpdateGatt", "polling")     //GUARDED: if was polling
                                                                    //then return to poll
    suspend fun updateGatt() {
        Debugger.printDebug(name, actorStringln("updating gatt..."))
        actorGattDescriptor = gattDescriptor.get()
        actorGattDescriptor?.gattCallback?.addOnRssiReaded(false, emitEventCallback)
        actorGattDescriptor?.gattCallback?.addOnRssiReaded(false, setInternalRssiVarCallback)
        actorGattDescriptor?.gattCallback?.addOnConnectionStateChange(false, emitEventOnStateChange)
        Debugger.printDebug(name, actorStringln("gatt updated [device=${actorGattDescriptor?.gatt?.device}]"))
    }

}