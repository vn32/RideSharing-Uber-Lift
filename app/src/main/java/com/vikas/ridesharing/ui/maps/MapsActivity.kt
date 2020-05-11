package com.vikas.ridesharing.ui.maps

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Transformations.map
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.vikas.ridesharing.R
import com.vikas.ridesharing.data.network.NetworkService
import com.vikas.ridesharing.utils.MapUtils
import com.vikas.ridesharing.utils.PermissionUtils
import com.vikas.ridesharing.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {
    companion object{
        private const val TAG="MapsActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE=999
        private const val PICKUP_REQUEST_CODE=1
        private const val DROP_REQUEST_CODE=2
    }
    //for showing cabs on view
    private val nearByCabsMarkerList= arrayListOf<Marker>()
    //variable for location access
    private var fusedLocationProviderClient:FusedLocationProviderClient?=null
    private lateinit var locationCallback: LocationCallback   //variable for location access
    private var currentLatLng: LatLng?=null   //variable for location access
    //for pickup and drop location
    private var pickUpLatLng:LatLng?=null
    private var dropLatLng:LatLng?=null
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
        setUpClickListener()
    }
    //for accessing current and drop location and adding clicklistener to layout file
    private fun setUpClickListener() {
        pickUpTextView.setOnClickListener{
        launchLocationAutoCompleteActivity(PICKUP_REQUEST_CODE)
        }
       dropTextView.setOnClickListener{
        launchLocationAutoCompleteActivity(DROP_REQUEST_CODE)
        }
        requestCabButton.setOnClickListener{//click listener for requesting a cab
            statusTextView.visibility=View.VISIBLE
            statusTextView.text=getString(R.string.requesting_your_cab)
            requestCabButton.isEnabled=false
            pickUpTextView.isEnabled=false
            dropTextView.isEnabled=false
            presenter.requestCab(pickUpLatLng!!,dropLatLng!!)
        }
    }
    //Think???
    private fun launchLocationAutoCompleteActivity(requestCode: Int){
        val fields:List<Place.Field> = listOf(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG)
        val intent= Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,fields).build(this)
        startActivityForResult(intent,requestCode)
        //requestcode for checking which response is coming either pickup or droptextview
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
    //creating marker for locations of cabs in view
    private fun addCarMarkerAndGet(latLng: LatLng):Marker{
        //created images of cabs
        val bitmapDescriptor=BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitMap(this))
        //setting this images to cabs location in view
        return googleMap.addMarker(MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor))
    }
    //for showing current location in layout
    private fun setCurrentLocationAsPick(){
        pickUpLatLng=currentLatLng
        pickUpTextView.text=getString(R.string.current_location)
    }

    //to show myself on location area
    fun enableMyLocationOnMap(){
        googleMap.setPadding(0,ViewUtils.dpToPx(48f),0,0)
        googleMap.isMyLocationEnabled=true

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
                            setCurrentLocationAsPick()
                            enableMyLocationOnMap()
                            moveCamera(currentLatLng)
                            animateCamera(currentLatLng)
                            //call nearbycabs here,Think!!!!!
                            presenter.requestNearByCabs(currentLatLng!!)
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
    //for requesting a cab button visible to user
    private fun checkAndShowRequestButton(){
         if(pickUpLatLng!=null && dropLatLng!=null){
             requestCabButton.visibility=View.VISIBLE//setting visibility of button
             requestCabButton.isEnabled=true
         }
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
    //we are getting data from autocompleteactivity inside the onactivityresult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== PICKUP_REQUEST_CODE || requestCode== DROP_REQUEST_CODE){
            when(resultCode){
                Activity.RESULT_OK -> {
                    val place=Autocomplete.getPlaceFromIntent(data!!)//data receive in parameter
                    when(requestCode){
                        PICKUP_REQUEST_CODE -> {
                            pickUpTextView.text=place.name
                            pickUpLatLng=place.latLng//storing picup loaction inside the variable
                            checkAndShowRequestButton()
                        }
                        DROP_REQUEST_CODE -> {
                            dropTextView.text=place.name
                            dropLatLng=place.latLng//storing drop location inside the varibale
                            checkAndShowRequestButton()
                        }

                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status: Status=Autocomplete.getStatusFromIntent(data!!)
                    Log.d(TAG,status.statusMessage)
                }
                Activity.RESULT_CANCELED -> {
                    //logging
                }
            }
        }
    }


    override fun onDestroy() {
        presenter.onDetach()//to disconect from networkservice before app destroy
        super.onDestroy()
    }

    override fun showNearByCabs(latLngList: List<LatLng>) {
        nearByCabsMarkerList.clear()//to erase the cache data
        for(latLng in latLngList){
            val nearByCabMarker=addCarMarkerAndGet(latLng)//created a marker for this location
            nearByCabsMarkerList.add(nearByCabMarker)//adding the marker int array


        }

    }

    override fun informCabBooked() {
        nearByCabsMarkerList.forEach{
            it.remove()//removing all marker of cab on map
        }
        nearByCabsMarkerList.clear()//clearing so that no buffer will left
        requestCabButton.visibility=View.GONE
        statusTextView.text=getString(R.string.your_cab_is_booked)
    }

    override fun showPath(latLngList: List<LatLng>) {

    }
}
