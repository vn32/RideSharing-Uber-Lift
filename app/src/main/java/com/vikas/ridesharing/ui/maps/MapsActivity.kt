package com.vikas.ridesharing.ui.maps

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Transformations.map
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.vikas.ridesharing.R
import com.vikas.ridesharing.data.network.NetworkService
import com.vikas.ridesharing.utils.PermissionUtils
import com.vikas.ridesharing.utils.ViewUtils

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {
    companion object{
        private const val TAG="MapsActivity"
    }
    private val LOCATION_PERMISSION_REQUEST_CODE=999
    private lateinit var presenter: MapsPresenter
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        ViewUtils.enableTransparentStatusBar(window)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //initializing presenter
        presenter= MapsPresenter(NetworkService())
        presenter.onAttach(this)//here this means MapsActivity(MapsView)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }
    //to check whether permission is given or not for location
    override fun onStart() {
        super.onStart()
        when{
            PermissionUtils.isAccessFineLocationGranted(this)->{
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        //fetch the location
                    } else -> {
                        PermissionUtils.showGPSNotEnabledDialog(this)
                    }
                }

            } else ->{
            PermissionUtils.requestAccessFineLocationPermission(this,LOCATION_PERMISSION_REQUEST_CODE)
                }
        }
    }
    //checking whether permision for location access is granted or not
    //if permission granted then checking whether gpslocationenable or not
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            LOCATION_PERMISSION_REQUEST_CODE ->{
                if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            //fetch the location
                        } else -> {
                        PermissionUtils.showGPSNotEnabledDialog(this)
                    }
                    }
                } else {
                    Toast.makeText(this,"Location Permission is not granted",Toast.LENGTH_LONG)
                        .show()
                }
            }

        }
    }

    override fun onDestroy() {
        presenter.onDetach()//to disconect from networkservice before app destroy
        super.onDestroy()
    }
}
