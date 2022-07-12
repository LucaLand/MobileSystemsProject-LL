package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothDevice
import java.util.*

class DiscoverySearchOptionsImpl() : DiscoverySearchOptions {

    lateinit var searcType : DiscoverySearchType
        private set
    var whenFound : (BluetoothDevice) -> Unit = {}
        private set
    lateinit var name : String
        private set
    lateinit var address : String
        private set
    lateinit var uuid : UUID
        private set
    lateinit var uuidString: String
        private set
    var disableDiscoveryAfterFound = true
        private set

    override fun findFirstThatHasName(name : String) {
        searcType = DiscoverySearchType.FIND_FIRST_NAME
        this.name = name
    }

    override fun findFirstThatHasAddress(address : String) {
        searcType = DiscoverySearchType.FIND_FIRST_ADDRESS
        this.address = address
    }

    override fun findFirstNameOrAddress(address : String, name : String) {
        searcType = DiscoverySearchType.FIND_FIRST_NAME_OR_ADDRESS
        this.address = address
        this.name = name
    }

    override fun findFirstNameAndAddress(address : String, name : String) {
        searcType = DiscoverySearchType.FIND_FIRST_NAME_AND_ADDRESS
        this.address = address
        this.name = name
    }

    override fun findFirstThatOffersService(uuid: String) {
        searcType = DiscoverySearchType.FIND_FIRST_OFFERING_SERVICE
        this.uuid = UUID.fromString(uuid)
        this.uuidString = uuid
    }

    override fun whenFound(action : (BluetoothDevice) -> Unit) {
        this.whenFound = action
    }

    override fun continueDiscoveryAfterFound() {
        this.disableDiscoveryAfterFound = false
    }

    override fun disableDiscoveryWhenFound() {
        this.disableDiscoveryAfterFound = true
    }

}