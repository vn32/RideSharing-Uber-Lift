package com.vikas.ridesharing.ui.maps

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
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
import com.vikas.ridesharing.utils.AnimationUtils
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
    private var greyPolyLine:Polyline?=null
    private var blackPolyLine:Polyline?=null
    lateinit var presenter: MapsPresenter
    private lateinit var googleMap: GoogleMap
    private var originMarker:Marker?=null
    private var destinationMarker:Marker?=null
    private var movingCabMarker:Marker?=null
    private var previousLatLngFromServer:LatLng?=null
    private var currentLatLngFromServer:LatLng?=null

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
        requestCabButton.setOnClickListener{
            statusTextView.visibility=View.VISIBLE
            statusTextView.text=getString(R.string.requesting_your_cab)
            requestCabButton.isEnabled=false
            pickUpTextView.isEnabled=false
            dropTextView.isEnabled=false
            presenter.requestCab(pickUpLatLng!!,dropLatLng!!)
        }
        nextRideButton.setOnClickListener{
            reset()
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
    //creating marker for source and destination
    private fun addOriginDestinationMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getDestinationBitMap())
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
    //button for next ride
    private fun reset() {
        statusTextView.visibility = View.GONE
        nextRideButton.visibility = View.GONE
        nearByCabsMarkerList.forEach {
            it.remove()
        }
        nearByCabsMarkerList.clear()
        currentLatLngFromServer = null
        previousLatLngFromServer = null
        if (currentLatLng != null) {
            moveCamera(currentLatLng)
            animateCamera(currentLatLng)
            setCurrentLocationAsPick()
            presenter.requestNearByCabs(currentLatLng!!)
        } else {
            pickUpTextView.text = ""
        }
        pickUpTextView.isEnabled = true
        dropTextView.isEnabled = true
        dropTextView.text = ""
        movingCabMarker?.remove()
        greyPolyLine?.remove()
        blackPolyLine?.remove()
        originMarker?.remove()
        destinationMarker?.remove()
        dropLatLng = null
        greyPolyLine = null
        blackPolyLine = null
        originMarker = null
        destinationMarker = null
        movingCabMarker = null

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
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)//
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
            nearByCabsMarkerList.forEach { it.remove() }
            nearByCabsMarkerList.clear()
            requestCabButton.visibility = View.GONE
            statusTextView.text = getString(R.string.status_cab_booked)
    }


    override fun showPath(latLngList: List<LatLng>) {
        val builder=LatLngBounds.Builder()//to create area to represent the locations
        for(latLng in latLngList){
            builder.include(latLng)
        }
        val bounds=builder.build()//bound is area with latlng
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,2))//2 is padding
        val polyLineOptions = PolylineOptions().apply {
            //as a marker to show path
            this.color(Color.GRAY)
            width(5f)
            addAll(latLngList)
        }
        greyPolyLine = googleMap.addPolyline(polyLineOptions)
        val blackPolyLineOptions = PolylineOptions().apply {
            color(Color.BLACK)
            width(5f)
        }
        blackPolyLine = googleMap.addPolyline(blackPolyLineOptions)
        originMarker = addOriginDestinationMarkerAndGet(latLngList[0])
        // Anchor ->centres the marker with respect to the path (based on given parameter value)
        originMarker?.setAnchor(.5f, .5f)
        destinationMarker = addOriginDestinationMarkerAndGet(latLngList[latLngList.size - 1])
        destinationMarker?.setAnchor(.5f, .5f)
        val polyLineAnimator = AnimationUtils.polyLineAnimator()
        //updating black line over grey
        polyLineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = valueAnimator.animatedValue as Int
            //using percentage as index for array of latlng upto which black color update
            val index = (greyPolyLine?.points!!.size) * (percentValue / 100f).toInt()
            blackPolyLine?.points = greyPolyLine?.points!!.subList(0, index)
        }
        polyLineAnimator.start()//to
    }
    //show driver current location car arrival
    override fun updateCabLocation(latLng: LatLng) {
        if(movingCabMarker==null){//we are checking if marker is null then we proceed else it will add infinite marker
            movingCabMarker=addCarMarkerAndGet(latLng)
        }
        if(previousLatLngFromServer==null){
            currentLatLngFromServer=latLng
            previousLatLngFromServer=currentLatLngFromServer
            movingCabMarker?.position=currentLatLngFromServer
            movingCabMarker?.setAnchor(0.5f,0.5f)
            animateCamera(currentLatLngFromServer)
        } else {
            previousLatLngFromServer=currentLatLngFromServer//like list node traversal
            currentLatLngFromServer=latLng
            val valueAnimator=AnimationUtils.cabAnimator()
            valueAnimator.addUpdateListener {va->
                if (currentLatLngFromServer!=null && previousLatLngFromServer!=null){
                    val multiplier=va.animatedFraction//1.......2
                    val nextLocation=LatLng(
                        multiplier*currentLatLngFromServer!!.latitude+(1-multiplier)*previousLatLngFromServer!!.latitude,
                        multiplier*currentLatLngFromServer!!.longitude+(1-multiplier)*previousLatLngFromServer!!.longitude
                    )
                    movingCabMarker?.position=nextLocation
                    val rotation=MapUtils.getRotation(previousLatLngFromServer!!,nextLocation)
                    if(!rotation.isNaN()){
                        movingCabMarker?.rotation=rotation
                    }
                    movingCabMarker?.setAnchor(0.5f,0.5f)
                    animateCamera(nextLocation)
                }


            }
            valueAnimator.start()
        }
    }

    override fun informCabIsArriving() {
        statusTextView.text=getString(R.string.your_cab_is_arriving)
    }

    override fun informCabArrived() {
        statusTextView.text=getString(R.string.your_cab_has_arrived)
        greyPolyLine?.remove()
        blackPolyLine?.remove()
        originMarker?.remove()
        destinationMarker?.remove()

    }

    override fun informTripStart() {
        statusTextView.text=getString(R.string.you_are_on_a_trip)
        previousLatLngFromServer=null//think? we wnat next ride from that position
    }

    override fun informTripEnd() {
        statusTextView.text=getString(R.string.trip_end)
        nextRideButton.visibility=View.VISIBLE//showing button next ride
        greyPolyLine?.remove()
        blackPolyLine?.remove()
        originMarker?.remove()
        destinationMarker?.remove()

    }
    //resetiing everything when any error occur
    override fun showRoutesNotAvailableError() {
        val error=getString(R.string.routes_not_available_choose_different_locations)
        Toast.makeText(this,error,Toast.LENGTH_LONG).show()
        reset()

    }

    override fun showDirectionApiFailedError(error: String) {
        Toast.makeText(this,error,Toast.LENGTH_LONG).show()
        reset()
    }

}

