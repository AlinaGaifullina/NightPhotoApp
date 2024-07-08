package ru.itis.nightphotoapp.utils

import android.graphics.Point
import android.view.WindowManager

class DisplayParameters {

    companion object{
        fun getScreenSize(windowManager: WindowManager): Point {
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            return size
        }
    }
}