package com.masabi.cloudfit.storage

interface ImageStore {
    fun put(bytes: ByteArray, contentType: String): String
    fun get(url: String): ByteArray
}
