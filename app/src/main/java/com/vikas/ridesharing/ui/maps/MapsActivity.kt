package com.vikas.ridesharing.ui.maps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Transformations.map
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.vikas.ridesharing.R
import com.vikas.ridesharing.data.network.NetworkService
import com.vikas.ridesharing.utils.ViewUtils

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {
    companion object{
        private const val TAG="MapsActivity"
    }
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
}
