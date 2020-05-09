package com.vikas.ridesharing.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.vikas.ridesharing.R

object MapUtils {
    //creating images of cabs
    fun getCarBitMap(context:Context):Bitmap{
        val bitMap=BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
        return Bitmap.createScaledBitmap(bitMap,50,100,false)
    }
}