package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothDevice

class DiscoverySearchOptionsImpl() : DiscoverySearchOptions {

    lateinit var searcType : DiscoverySearchType
        private set
    var whenFound : (BluetoothDevice) -> Unit = {}
        private set
    lateinit var name : String
        private set
    lateinit var address : String
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

    override fun whenFound(action : (BluetoothDevice) -> Unit) {
        this.whenFound = action
    }

}