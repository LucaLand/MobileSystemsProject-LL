package it.unibo.mobilesystems.bluetooth

import android.bluetooth.BluetoothDevice

enum class DiscoverySearchType {
    FIND_FIRST_NAME, FIND_FIRST_ADDRESS, FIND_FIRST_NAME_OR_ADDRESS,
    FIND_FIRST_NAME_AND_ADDRESS, FIND_FIRST_OFFERING_SERVICE
}

interface DiscoverySearchOptions {

    fun findFirstThatHasName(name : String)
    fun findFirstThatHasAddress(address : String)
    fun findFirstNameOrAddress(address : String, name : String)
    fun findFirstNameAndAddress(address : String, name : String)
    fun findFirstThatOffersService(uuid : String)
    fun whenFound(action : (BluetoothDevice) -> Unit)
    fun disableDiscoveryWhenFound()
    fun continueDiscoveryAfterFound()

}