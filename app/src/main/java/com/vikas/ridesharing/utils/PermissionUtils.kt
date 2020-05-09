package com.vikas.ridesharing.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    fun requestAccessFineLocationPermission(activity:AppCompatActivity,requestId:Int){
        ActivityCompat.requestPermissions(activity,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),requestId)
    }
    //function to check permission granted or not
    fun isAccessFineLocationGranted(context: Context):Boolean{
        return ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED

    }
    //check if location is enable or not, else showGpsEnable dialog
    fun isLocationEnabled(context: Context):Boolean{
        val locationManager:LocationManager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    fun showGPSNotEnabledDialog(context: Context){
        AlertDialog.Builder(context)
            .setTitle("Enabled GPS")
            .setMessage("Required for this app")
            .setCancelable(false)
            .setPositiveButton("Enabled Now"){_,_->
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

            }
            .show()
    }
}