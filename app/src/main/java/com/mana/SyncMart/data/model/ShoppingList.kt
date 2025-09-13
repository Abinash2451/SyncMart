package com.mana.SyncMart.data.model

data class ShoppingList(
    val id: String = "",
    val name: String = "",
    val items: List<Item> = emptyList(),
    val ownerId: String = "",
    val sharedWith: List<String> = emptyList()
)
