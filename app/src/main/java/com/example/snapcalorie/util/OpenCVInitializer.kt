package com.example.snapcalorie.util

import android.content.Context
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

object OpenCVInitializer {
    private var isInitialized = false
    
    fun initializeOpenCV(context: Context, onInitialized: () -> Unit) {
        if (isInitialized) {
            onInitialized()
            return
        }
        
        val loaderCallback = object : BaseLoaderCallback(context) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        isInitialized = true
                        onInitialized()
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }
        
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, loaderCallback)
        } else {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
    
    fun isOpenCVInitialized(): Boolean = isInitialized
} 