package it.unibo.mobilesystems.permissionManager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import it.unibo.mobilesystems.debugUtils.Debugger
import it.unibo.mobilesystems.debugUtils.DebuggerContextNameAnnotation


// https://stackoverflow.com/questions/35484767/activitycompat-requestpermissions-not-showing-dialog-box

object PermissionsManager {

    //TODO (Use the more generic function i've made, for different Permission tasks - Check & Ask)

    var version = Build.VERSION.SDK_INT


    /** ----------PUBLIC FUNCTIONS---------------- */
    fun permissionCheck(permissionType: PermissionType, context : AppCompatActivity) : Boolean{
        if(permissionType == PermissionType.Bluetooth)
            return checkBluetoothPermission(context)
        else if(permissionType == PermissionType.Location)
            return checkLocationPermission(context)
        return false
    }

    //@DebuggerContextNameAnnotation("Permission Check")
    fun permissionsCheck(context: AppCompatActivity, permissions: Array<out String>) : Boolean{
        permissions.forEach { permission ->
            if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                Debugger.printDebug("Permission [${permission}] - NOT Granted ")

                return false
            }else{
                Debugger.printDebug("Permission [${permission}] - Granted (top) ")
            }
        }
        return true
    }

    fun permissionsRequest(context: AppCompatActivity, permissions : Array<out String>, intCode : Int){
        ActivityCompat.requestPermissions(context, permissions, intCode)
    }

    /** THE FUNCTION OnPermissionRequestResult IS OVERRIDED IN EVERY
     * ACTIVITY THAT NEED IT (CALLING THIS FUNC)                        */
    fun onPermissionsRequestResult(){

    }

    /** ----------Private FUNCTIONS (Business Logic)---------------- */

    private fun checkLocationPermission(context: AppCompatActivity) : Boolean {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )!= PackageManager.PERMISSION_GRANTED
        ) {
            Debugger.printDebug("Location Permission NOT GRANTED CORRECTLY [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION]")
            permissionsRequest(context, permissions, PermissionType.Location.ordinal)
            false
        }else{
            Debugger.printDebug("Location Permission GRANTED CORRECTLY [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION]")
            true
        }

    }

    private fun checkBluetoothPermission(context : AppCompatActivity) : Boolean{
        if(version >= 31) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Debugger.printDebug("Bluetooth Permission: NOT GRANTED CORRECTLY [BLUETOOTH_CONNECT, BLUETOOTH_SCAN]")
                permissionsRequest(context, permissions, PermissionType.Bluetooth.ordinal)
                return false
            } else {
                Debugger.printDebug("Bluetooth Permission: GRANTED CORRECTLY [BLUETOOTH_CONNECT, BLUETOOTH_SCAN]")
                return true
            }
        }
        else {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Debugger.printDebug("Bluetooth Permission NOT GRANTED CORRECTLY [BLUETOOTH, BLUETOOTH_ADMIN]")
                permissionsRequest(context, permissions, PermissionType.Bluetooth.ordinal)
                //permissionRequest(PermissionType.Bluetooth, context)
                return false
            } else {
                Debugger.printDebug("Bluetooth Permission GRANTED CORRECTLY [BLUETOOTH_CONNECT, BLUETOOTH_ADMIN]")
                return true
            }
        }

    }

}