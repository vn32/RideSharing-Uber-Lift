package com.vikas.ridesharing.ui.maps

import android.util.Log
import com.vikas.ridesharing.data.network.NetworkService
import com.vikas.ridesharing.simulator.WebSocket
import com.vikas.ridesharing.simulator.WebSocketListener

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
    //For disconnecting from websocket
    fun onDetach(){
        webSocket.disconnect()
        view=null
    }
    override fun onConnect() {
        Log.d(TAG,"onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG,"onMessage data:$data")
    }

    override fun onDisconnect() {
        Log.d(TAG,"onDisconnect")

    }

    override fun onError(error: String) {
        Log.d(TAG,"onError error:$error")
    }
}