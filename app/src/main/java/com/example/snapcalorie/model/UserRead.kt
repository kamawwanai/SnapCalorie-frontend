package com.example.snapcalorie.model

data class UserRead(
    val id: Int,
    val email: String,
    val registered_at: String  // в ISO-формате, типа "2025-05-14T12:34:56.789000"
)
