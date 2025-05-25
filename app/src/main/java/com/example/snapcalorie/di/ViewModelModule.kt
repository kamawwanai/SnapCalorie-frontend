package com.example.snapcalorie.di

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.snapcalorie.viewmodel.OnboardingPlanViewModel

object ViewModelModule {
    val Factory = viewModelFactory {
        initializer {
            val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
            OnboardingPlanViewModel(application)
        }
    }
} 