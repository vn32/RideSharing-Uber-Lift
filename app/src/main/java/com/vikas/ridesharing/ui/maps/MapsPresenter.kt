package com.vikas.ridesharing.ui.maps

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.vikas.ridesharing.data.network.NetworkService
import com.vikas.ridesharing.simulator.WebSocket
import com.vikas.ridesharing.simulator.WebSocketListener
import com.vikas.ridesharing.utils.Constants
import org.json.JSONObject

class MapsPresenter (private val networkService:NetworkService):WebSocketListener{
    companion object{
        private const val TAG="MapsPresenter"
    }
    private  var view:MapsView?=null
    private lateinit var webSocket:WebSocket
    fun onAttach(view:MapsView){
        this.view=view  //attaching external view to MapsView
        webSocket=networkService.CreateWebSocket(this)//initializing websocket
        webSocket.connect()
    }
    private fun handleOnMessageNearByCabs(jsonObject: JSONObject){
        val nearByCabLocations= arrayListOf<LatLng>()
        //get all locations of cabs
        val jsonArray=jsonObject.getJSONArray(Constants.LOCATIONS)
        for(i in 0 until  jsonArray.length()){
            val lat=(jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            val lng=(jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            val latLng=LatLng(lat,lng)
            nearByCabLocations.add(latLng)
        }
        //put all cab locations into view
        view?.showNearByCabs(nearByCabLocations)
    }
    //For disconnecting from websocket
    fun onDetach(){
        webSocket.disconnect()
        view=null
    }
    override fun onConnect() {
        Log.d(TAG,"onConnect")
    }
    //server sending cabs information nearby my location
    override fun onMessage(data: String) {
        Log.d(TAG,"onMessage data:$data")
        val jsonObject=JSONObject(data)//converting string data into json format??
        when(jsonObject.getString(Constants.TYPE)){
            Constants.NEAR_BY_CABS ->{
                //for storing the all locations of cabs nearby me
                handleOnMessageNearByCabs(jsonObject)
            }
            Constants.CAB_BOOKED ->{
                Log.d(TAG,Constants.CAB_BOOKED)
                view?.informCabBooked()
            }
            Constants.PICKUP_PATH -> {
                val jsonArray=jsonObject.getJSONArray("path")//taking all location send by server
                val pickUpPath= arrayListOf<LatLng>()
                for(i in 0 until  jsonArray.length()){
                    val lat=(jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
                    val lng=(jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
                    val latLng=LatLng(lat,lng)
                   pickUpPath.add(latLng)
                }
                view?.showPath(pickUpPath)


            }
            Constants.LOCATION -> {
                val latCurrent=jsonObject.getDouble("lat")
                val lngCurrent=jsonObject.getDouble("lng")
                //notify the view
                view?.updateCabLocation(LatLng(latCurrent,lngCurrent))
            }
            Constants.CAB_IS_ARRIVING ->{
                view?.informCabIsArriving()
            }
            Constants.CAB_ARRIVED ->{
                view?.informCabArrived()
            }
        }

    }

    override fun onDisconnect() {
        Log.d(TAG,"onDisconnect")

    }

    override fun onError(error: String) {
        Log.d(TAG,"onError error:$error")
    }
    //sending request to server of type nearbycabs with my current location to find all cabs near to me
    fun requestNearByCabs(latLng: LatLng)
    {
        val jsonObject=JSONObject()
        jsonObject.put(Constants.TYPE,Constants.NEAR_BY_CABS)
        jsonObject.put(Constants.LAT,latLng.latitude)
        jsonObject.put(Constants.LNG,latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }
    //requesting a cab from server
    fun requestCab(pickUpLatLng: LatLng,dropLatLng: LatLng){
        Log.d(TAG,Constants.REQUEST_CAB)
        val jsonObject = JSONObject().apply {
            put(Constants.TYPE, Constants.REQUEST_CAB)
            put("pickUpLat",pickUpLatLng.latitude)
            put("pickUpLng", pickUpLatLng.longitude)
            put("dropLat", dropLatLng.latitude)
            put("dropLng", dropLatLng.longitude)
        }
        webSocket.sendMessage(jsonObject.toString())
      //sending message to server in string type

    }
}