package com.usher.demo.api

import com.usher.demo.api.entities.CartEntity
import com.usher.demo.api.entities.DetailEntity
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET

interface ApiService {

    @GET("api/v1/detail")
    fun getDetail(): Observable<DetailEntity>

    @GET("api/v1/cart")
    fun getCart(): Observable<CartEntity>
}