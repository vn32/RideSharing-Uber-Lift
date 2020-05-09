package com.vikas.ridesharing.data.network

import com.vikas.ridesharing.simulator.WebSocket
import com.vikas.ridesharing.simulator.WebSocketListener

class NetworkService {
    fun CreateWebSocket(WebSocketListener:WebSocketListener):WebSocket{
        return WebSocket(WebSocketListener)
    }
}