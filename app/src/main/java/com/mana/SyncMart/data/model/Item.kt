package com.mana.SyncMart.data.model

data class Item(
    val id: String = "",
    val name: String = "",
    val quantity: String = "1", // Changed from Int to String to allow units
    val purchased: Boolean = false
)