package com.example.snapcalorie.storage

import android.content.Context

class TokenStorage(context: Context) {
    private val prefs = context
        .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("jwt_token", null)
        set(value) = prefs.edit().putString("jwt_token", value).apply()

    fun clear() {
        prefs.edit().remove("jwt_token").apply()
    }
}
