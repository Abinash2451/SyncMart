package com.mana.SyncMart.data.model

data class Item(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val purchased: Boolean = false
)
