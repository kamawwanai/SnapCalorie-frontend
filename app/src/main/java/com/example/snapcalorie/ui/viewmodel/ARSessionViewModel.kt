package com.example.snapcalorie.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.snapcalorie.ui.screens.ARCaptureData
import com.example.snapcalorie.ui.screens.ARSegmentationResult

class ARSessionViewModel : ViewModel() {
    
    var topViewCaptureData: ARCaptureData? = null
        private set
    
    var sideViewCaptureData: ARCaptureData? = null
        private set
    
    var topViewSegmentationResult: ARSegmentationResult? = null
        private set
    
    var sideViewSegmentationResult: ARSegmentationResult? = null
        private set
    
    fun setTopViewCaptureData(data: ARCaptureData) {
        Log.d("ARSessionViewModel", "Setting top view capture data: ${data.rgbFrame.width}x${data.rgbFrame.height}")
        topViewCaptureData = data
        Log.d("ARSessionViewModel", "Top view capture data set successfully")
    }
    
    fun setSideViewCaptureData(data: ARCaptureData) {
        Log.d("ARSessionViewModel", "Setting side view capture data: ${data.rgbFrame.width}x${data.rgbFrame.height}")
        sideViewCaptureData = data
        Log.d("ARSessionViewModel", "Side view capture data set successfully")
    }
    
    fun setTopViewSegmentationResult(result: ARSegmentationResult) {
        Log.d("ARSessionViewModel", "Setting top view segmentation result")
        topViewSegmentationResult = result
        Log.d("ARSessionViewModel", "Top view segmentation result set successfully")
    }
    
    fun setSideViewSegmentationResult(result: ARSegmentationResult) {
        Log.d("ARSessionViewModel", "Setting side view segmentation result")
        sideViewSegmentationResult = result
        Log.d("ARSessionViewModel", "Side view segmentation result set successfully")
    }
    
    fun clearSession() {
        Log.d("ARSessionViewModel", "Clearing AR session data...")
        topViewCaptureData = null
        sideViewCaptureData = null
        topViewSegmentationResult = null
        sideViewSegmentationResult = null
        Log.d("ARSessionViewModel", "AR session cleared successfully")
    }
    
    fun isTopViewComplete(): Boolean {
        return topViewSegmentationResult != null
    }
    
    fun isSideViewComplete(): Boolean {
        return sideViewSegmentationResult != null
    }
    
    fun isSessionComplete(): Boolean {
        return isTopViewComplete() && isSideViewComplete()
    }
    
    fun getTopViewClassificationResult(): String? {
        return topViewSegmentationResult?.classificationResult
    }
} 