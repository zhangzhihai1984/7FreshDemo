package com.usher.demo.api.entities

data class DetailEntity(
    val skuId: Int,
    val productId: Int,
    val skuName: String,
    val jdPrice: String,
    val buyUnit: String,
    val adText: String,
    val imageUrls: List<String>,
    val jdShipment: ShipmentEntity,
    val detailUrl: String
)

data class ShipmentEntity(val address: String, val deliveryDateAndTime: String)