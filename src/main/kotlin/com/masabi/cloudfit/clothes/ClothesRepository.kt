package com.masabi.cloudfit.clothes

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClothesRepository {

    private val store = ConcurrentHashMap<String, Clothe>()

    fun all(): List<Clothe> = store.values.toList()
    fun get(id: String): Clothe? = store[id]
    fun save(clothe: Clothe): Clothe {
        val stored = clothe.copy(id = clothe.id.ifBlank { UUID.randomUUID().toString() })
        store[stored.id] = stored
        return stored
    }
    fun delete(id: String) {
        store.remove(id)
    }
}
