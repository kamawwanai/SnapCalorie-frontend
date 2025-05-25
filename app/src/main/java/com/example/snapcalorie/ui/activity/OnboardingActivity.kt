package com.example.snapcalorie.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.snapcalorie.storage.OnboardingStorage
import com.example.snapcalorie.ui.screens.onboarding.OnboardingPager
import com.example.snapcalorie.ui.theme.SnapCalorieTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("OnboardingActivity", "OnboardingActivity started")
        
        setContent {
            SnapCalorieTheme {
                OnboardingPager(
                    onFinish = {
                        Log.d("OnboardingActivity", "Onboarding finished - setting completed flag")
                        OnboardingStorage(this@OnboardingActivity).isInitialOnboardingCompleted = true
                        Log.d("OnboardingActivity", "Starting LoginActivity")
                        startActivity(Intent(this@OnboardingActivity, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
} 