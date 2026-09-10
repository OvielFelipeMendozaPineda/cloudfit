package com.masabi.cloudfit.outfit

import java.util.concurrent.ConcurrentHashMap

class OutfitsRepository {
    private val store = ConcurrentHashMap<String, Outfit>()

    fun all(): List<Outfit> = store.values.toList()
    fun get(id: String): Outfit? = store[id]
    fun save(outfit: Outfit): Outfit {
        store[outfit.id] = outfit
        return outfit
    }
}
