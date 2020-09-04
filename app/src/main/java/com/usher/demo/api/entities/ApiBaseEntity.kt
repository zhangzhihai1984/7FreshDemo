package com.usher.demo.api.entities

class ErrorEntity constructor(val status: Int, val title: String) {
    val type: String? = null
    val detail: String? = null
    val path: String? = null

    val error: String? = null
    val error_description: String? = null
}

class ResultEntity<T>(val data: T?, error: ErrorEntity?) : BaseResultEntity(error)

open class BaseResultEntity(val error: ErrorEntity?)