package com.example.reactive

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import reactor.cache.CacheFlux
import reactor.cache.CacheMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import reactor.test.StepVerifier
import redis.embedded.RedisServer
import java.util.ArrayList

typealias CachedList<T> = Mono<List<Signal<T>>>


class CacheFluxExplorationTest {

    data class Customer(val id: Long, val name: String)

    val cacheMap = mutableMapOf<String, Customer>()

    val customersInCache = listOf(
            "1" to Customer(1, "Danny"),
            "2" to Customer(2, "Stephan"),
            "3" to Customer(3, "Thomas")
    )

    val customerNotInCache = Customer(4, "Mr. No-Cache")

    //TODO Test that fallback is not called on cache-miss

    @Test
    fun testCacheFlux() {
        cacheMap.putAll(customersInCache)

        StepVerifier.create(cacheFluxAccess(1))
                .expectNext(Customer(1, "Danny"))
                .verifyComplete()

        StepVerifier.create(cacheFluxAccess(4))
                .expectNext(customerNotInCache)
                .verifyComplete()

        assert(cacheMap.containsKey("4"))

        cacheMap.clear()
    }

    @Test
    fun testCacheMono() {

        cacheMap.putAll(customersInCache)

        StepVerifier.create(cachedMonoAccess(1))
                .expectNext(Customer(1, "Danny"))
                .verifyComplete()

        StepVerifier.create(cachedMonoAccess(4))
                .expectNext(customerNotInCache)
                .verifyComplete()

        assert(cacheMap.containsKey("4"))

        cacheMap.clear()
    }

    fun cachedMonoAccess(id: Long): Mono<Customer> = CacheMono
            .lookup(::monoReader, id.toString())
            .onCacheMissResume { getById(id) }
            .andWriteWith { k, v -> monoWriter(k, v) }

    fun cacheFluxAccess(id: Long): Flux<Customer> = CacheFlux
            .lookup(::fluxReader, id.toString())
            .onCacheMissResume { Flux.from(getById(id)) }
            .andWriteWith { k, v -> fluxWriter(k, v) }

    fun fluxReader(key: String) =
            cacheMap[key]?.let { Flux.fromIterable(listOf(it)).materialize().collectList() } ?: Mono.empty()


    fun fluxWriter(key: String, values: List<Signal<Customer>>) = Flux.fromIterable(values).dematerialize<Customer>()
            .doOnNext { customer -> cacheMap.put(key, customer) }
            .then()


    fun monoReader(key: String): Mono<Signal<out Customer>> =
            Mono.justOrEmpty(cacheMap[key]).map { Signal.next(it) }


    fun monoWriter(key: String, value: Signal<out Customer>) =
            value.get()?.let { Mono.justOrEmpty(cacheMap.put(key, it)).then() }


    fun getById(id: Long): Mono<Customer> = when (id) {
        4L -> Mono.just(Customer(4, "Mr. No-Cache"))
        else -> Mono.empty()
    }

}