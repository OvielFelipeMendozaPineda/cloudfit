package com.masabi.cloudfit.avatar

class AvatarRepository {
    @Volatile
    private var current: Avatar? = null

    fun get(): Avatar? = current

    fun set(avatar: Avatar): Avatar {
        current = avatar
        return avatar
    }

    fun clear() {
        current = null
    }
}
