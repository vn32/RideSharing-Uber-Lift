package com.vikas.ridesharing.ui.maps

import com.google.android.gms.maps.model.LatLng

interface MapsView {
    //for checking location of cabs near to me
    fun showNearByCabs(latLngList:List<LatLng>)
}