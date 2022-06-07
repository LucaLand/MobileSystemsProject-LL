package it.unibo.mobilesystems.permissionManager

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

object PermissionsManager {

    fun permissionRequest(permissionType: PermissionType, app: AppCompatActivity) {
        permissionRequest(permissionType.name, app)
    }

    fun permissionRequest(permissionType: String, app : AppCompatActivity): Boolean {
        //  Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        if (permissionType == PermissionType.Location.name) {
            return requestLocationPermission(app)
        } else if (permissionType == PermissionType.Bluetooth.name)
            return requestBluetoothPermission()

        return false
    }

    private fun requestBluetoothPermission(): Boolean {
        TODO("Not yet implemented")
    }



    private fun requestLocationPermission(app: AppCompatActivity): Boolean {
         var granted : Boolean = false
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