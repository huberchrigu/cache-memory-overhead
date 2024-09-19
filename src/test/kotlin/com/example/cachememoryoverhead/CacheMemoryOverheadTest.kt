package com.example.cachememoryoverhead

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import kotlin.time.measureTime

private const val ITEMS = 50_000

class CacheMemoryOverheadTest {
    private val expected = (0 until ITEMS).joinToString("")

    @Test
    fun `demonstrate memory overhead of cache()`() {
        runTest({ Flux.range(0, ITEMS) }, { it.next().block() }) { it.skip(1).cache() }
    }

    @Test
    fun `demonstrate flux to iterator usage()`() {
        runTest({ Flux.range(0, ITEMS).toIterable().iterator() }, { if (it.hasNext()) it.next() else null }) { it }
    }

    @Test
    fun `demonstrate no memory issue with list()`() {
        runTest({ (0 until ITEMS).toList() }, { it.firstOrNull() }, { it.drop(1) })
    }


    @Test
    fun `demonstrate better performance with list()`() {
        var i = 0
        runTest({ (0 until ITEMS).toList() }, { if (i >= it.size) null else it[i] }, { i++; it })
    }


    @Test
    fun `demonstrate better performance with iterator()`() {
        runTest({ (0 until ITEMS).asSequence().iterator() }, { if (it.hasNext()) it.next() else null }, { it })
    }

    private fun <T> runTest(init: () -> T, getNext: (T) -> Int?, update: (T) -> T) {
        val initialMemory = getUsedMemory()

        var items = init()
        var collect = ""

        val time = measureTime {
            while (true) {
                val next = getNext(items) ?: break
                collect += next
                items = update(items)
            }
        }

        val finalMemory = getUsedMemory()
        val memoryDifference = (finalMemory - initialMemory) / 1024

        println("Memory difference: $memoryDifference kB")
        println("Time: $time")
        assertThat(collect).isEqualTo(expected)
    }

    private fun getUsedMemory(): Long {
        System.gc()
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}
