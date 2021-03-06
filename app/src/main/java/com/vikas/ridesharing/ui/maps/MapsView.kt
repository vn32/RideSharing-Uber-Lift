package com.vikas.ridesharing.ui.maps

import com.google.android.gms.maps.model.LatLng

interface MapsView {
    //for checking location of cabs near to me
    fun showNearByCabs(latLngList:List<LatLng>)
    fun informCabBooked()
    fun showPath(latLngList: List<LatLng>)
    fun updateCabLocation(latLng: LatLng)
    fun informCabIsArriving()
    fun informCabArrived()
    fun informTripStart()
    fun informTripEnd()
    fun showRoutesNotAvailableError()
    fun showDirectionApiFailedError(error:String)
}