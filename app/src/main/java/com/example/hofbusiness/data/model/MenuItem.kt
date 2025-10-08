package com.example.hofbusiness.data.model

data class MenuItem(
    val id: String = "",
    val name: String = "",
    val price1000g: Int = 0,
    val price500g: Int = 0,
    val price250g: Int = 0,
    val isAvailable: Boolean = true
) {
    fun getPriceForSize(size: String): Int {
        return when (size) {
            "250g" -> price250g
            "500g" -> price500g
            "1000g" -> price1000g
            else -> 0
        }
    }
}
