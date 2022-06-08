package it.unibo.mobilesystems.permissionManager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import it.unibo.mobilesystems.debugUtils.debugger


// https://stackoverflow.com/questions/35484767/activitycompat-requestpermissions-not-showing-dialog-box

object PermissionsManager {

    var version = Build.VERSION.SDK_INT

    fun permissionCheck(permissionType: PermissionType, context : AppCompatActivity) : Boolean{
        if(permissionType == PermissionType.Bluetooth)
            return checkBluetoothPermission(context)
        else if(permissionType == PermissionType.Location)
            return checkLocationPermission(context)

        return false
    }

    private fun checkLocationPermission(context: AppCompatActivity) : Boolean {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )!= PackageManager.PERMISSION_GRANTED
        ) {
            debugger.printDebug("Location Permission NOT GRANTED CORRECTLY [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION]")
            permissionsRequest(context, permissions, PermissionType.Location.ordinal)
            return true
        }else{
            debugger.printDebug("Bluetooth Permission GRANTED CORRECTLY [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION]")
            return true
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
                debugger.printDebug("Bluetooth Permission: NOT GRANTED CORRECTLY [BLUETOOTH_CONNECT, BLUETOOTH_SCAN]")
                permissionsRequest(context, permissions, PermissionType.Bluetooth.ordinal)
                return false
            } else {
                debugger.printDebug("Bluetooth Permission: GRANTED CORRECTLY [BLUETOOTH_CONNECT, BLUETOOTH_SCAN]")
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
                debugger.printDebug("Bluetooth Permission NOT GRANTED CORRECTLY [BLUETOOTH, BLUETOOTH_ADMIN]")
                permissionsRequest(context, permissions, PermissionType.Bluetooth.ordinal)
                //permissionRequest(PermissionType.Bluetooth, context)
                return false
            } else {
                debugger.printDebug("Bluetooth Permission GRANTED CORRECTLY [BLUETOOTH_CONNECT, BLUETOOTH_ADMIN]")
                return true
            }
        }

    }

    fun permissionsRequest(context: AppCompatActivity, permissions : Array<out String>, intCode : Int){
        ActivityCompat.requestPermissions(context, permissions, intCode)
    }



    private fun requestLocationPermission(app: AppCompatActivity): Boolean {
        var granted = false
        val locationPermissionRequest = app.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            granted = when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    true  // Precise location access granted.
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    false // Only approximate location access granted.
                } else -> {
                    false     // No location access granted.
                }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))

        return granted
    }



}