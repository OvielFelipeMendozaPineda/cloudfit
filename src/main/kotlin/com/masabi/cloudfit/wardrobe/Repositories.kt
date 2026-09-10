package com.masabi.cloudfit.wardrobe

import com.masabi.cloudfit.shared.Clothe
import com.masabi.cloudfit.shared.Event
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface ClosetRepository {
    fun all(): List<Clothe>
    fun get(id: String): Clothe?
    fun save(clothe: Clothe): Clothe
}

interface EventRepository {
    fun all(): List<Event>
    fun get(id: String): Event?
    fun save(event: Event): Event
}

class InMemoryClosetRepository : ClosetRepository {
    private val store = ConcurrentHashMap<String, Clothe>()
    override fun all(): List<Clothe> = store.values.toList()
    override fun get(id: String): Clothe? = store[id]
    override fun save(clothe: Clothe): Clothe {
        val stored = clothe.copy(id = clothe.id.ifBlank { UUID.randomUUID().toString() })
        store[stored.id] = stored
        return stored
    }
}

class InMemoryEventRepository : EventRepository {
    private val store = ConcurrentHashMap<String, Event>()
    override fun all(): List<Event> = store.values.toList()
    override fun get(id: String): Event? = store[id]
    override fun save(event: Event): Event {
        val stored = event.copy(id = event.id.ifBlank { UUID.randomUUID().toString() })
        store[stored.id] = stored
        return stored
    }
}
