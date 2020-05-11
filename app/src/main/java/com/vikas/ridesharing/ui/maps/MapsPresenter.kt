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
            //for checking wheteher cab is booked or not
            Constants.CAB_BOOKED ->{
                view?.informCabBooked()
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
        val jsonObject=JSONObject() //creating object to send message to server
        jsonObject.put(Constants.TYPE,Constants.REQUEST_CABS)
        jsonObject.put("pickUpLat",pickUpLatLng.latitude)
        jsonObject.put("pickUpLng",pickUpLatLng.longitude)
        jsonObject.put("dropLat",dropLatLng.latitude)
        jsonObject.put("dropLng",dropLatLng.longitude)
        webSocket.sendMessage(jsonObject.toString())//sending message to server in string type

    }
}