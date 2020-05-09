package com.vikas.ridesharing.ui.maps

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Transformations.map
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.vikas.ridesharing.R
import com.vikas.ridesharing.data.network.NetworkService
import com.vikas.ridesharing.utils.PermissionUtils
import com.vikas.ridesharing.utils.ViewUtils

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {
    companion object{
        private const val TAG="MapsActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE=999
    }
    //variable for location access
    private var fusedLocationProviderClient:FusedLocationProviderClient?=null
    private lateinit var locationCallback: LocationCallback   //variable for location access
    private var currentLatLng: LatLng?=null   //variable for location access

    lateinit var presenter: MapsPresenter
    private lateinit var googleMap: GoogleMap

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
    //move camera at current LatLng
    private fun moveCamera(latLng: LatLng?){
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    }
    //animate camera with movement
    private fun animateCamera(latLng: LatLng?){
        val cameraPosition=CameraPosition.Builder().target(latLng).zoom(15.5f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

    }
    fun enableMyLocation(){
        //discuss later
    }
    //to access the the current location
    fun setUpLocationListener(){
        fusedLocationProviderClient= FusedLocationProviderClient(this)
        //for getting the current location update every 2 seconds
        val locationRequest=LocationRequest().setInterval(2000).setFastestInterval(2000)
        locationCallback= object :LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if(currentLatLng==null){
                    for(location in locationResult.locations){
                        if(currentLatLng==null){
                            currentLatLng= LatLng(location.latitude,location.longitude)
                            enableMyLocation()
                            moveCamera(currentLatLng)
                            animateCamera(currentLatLng)
                        }

                    }
                }
                //we can update the location of user on server
            }
            
        }
        //binding locationcall with fusedlocationprovederclient
        fusedLocationProviderClient?.requestLocationUpdates(locationRequest,locationCallback,
            Looper.myLooper())
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }
    //to check whether permission is given or not for location
    override fun onStart() {
        super.onStart()
        when{
            PermissionUtils.isAccessFineLocationGranted(this)->{
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        //fetch the location
                        setUpLocationListener()
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
                            setUpLocationListener()
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
