package com.vikas.ridesharing.utils

import android.content.Context
import android.graphics.*
import com.google.android.gms.maps.model.LatLng
import com.vikas.ridesharing.R
import kotlin.math.abs
import kotlin.math.atan

object MapUtils {
    //creating images of cabs
    fun getCarBitMap(context:Context):Bitmap{
        val bitMap=BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
        return Bitmap.createScaledBitmap(bitMap,50,100,false)
    }
//creating marker for destination
    fun getDestinationBitMap(): Bitmap {
        val height = 20
        val width = 20
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
    fun getRotation(start: LatLng, end: LatLng): Float {
        val latDifference = abs(start.latitude - end.latitude)
        val lngDifference = abs(start.longitude - end.longitude)
        var rotation = -1f

        when {
            start.latitude < end.latitude && start.longitude < end.longitude -> {
                rotation = Math.toDegrees(atan(lngDifference / latDifference)).toFloat()
            }
            start.latitude >= end.latitude && start.longitude < end.longitude -> {
                rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 90).toFloat()
            }
            start.latitude >= end.latitude && start.longitude >= end.longitude -> {
                rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 180).toFloat()
            }
            start.latitude < end.latitude && start.longitude >= end.longitude -> {
                rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 270).toFloat()
            }
        }
        return rotation
    }
}