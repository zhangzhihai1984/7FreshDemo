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

data class CartEntity(
        val storeId: Int,
        val tenantId: Int,
        val storeName: String,
        val cartItems: List<CartItemEntity>
)

/**
 * 规格 0.350kg
 * ¥9.9/盒
 */
data class CartItemEntity(
        val inCartId: Int,
        val orderLineId: String,
        val skuName: String,
        val unitPrice: Float,       //9.9
        val totalPrice: String,
        val status: Int,
        val weightSku: Boolean,     //0.350
        val weight: String,         //kg
        val weightUnit: String,
        val imageUrl: String,
        val buyUnit: String,        //盒
        val buyNum: Int
)