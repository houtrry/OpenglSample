package com.houtrry.lopengles20.tile

import java.util.LinkedHashMap
import java.util.Map

/**
 * 最小 LRU 缓存：
 * - 使用访问顺序的 LinkedHashMap；
 * - 超出容量自动移除最久未使用的条目；
 * - 线程安全：方法级同步，满足轻量使用需求。
 */
class LruCache<K, V>(private val maxSize: Int) {
    private val map: LinkedHashMap<K, V> = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V): V? = map.put(key, value)

    @Synchronized
    fun remove(key: K): V? = map.remove(key)

    @Synchronized
    fun keys(): Set<K> = map.keys

    @Synchronized
    fun size(): Int = map.size
}


