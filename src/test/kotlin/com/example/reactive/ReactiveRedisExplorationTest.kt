package com.example.reactive

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import redis.embedded.RedisServer
import java.time.Duration

class ReactiveRedisExplorationTest {

    data class Customer(val id: Long, val name: String)

    val factory: ReactiveRedisConnectionFactory = LettuceConnectionFactory("localhost", redisPort).apply {
        afterPropertiesSet()
    }

    val serializer = Jackson2JsonRedisSerializer<Customer>(Customer::class.java).apply {
        setObjectMapper(jacksonObjectMapper())
    }

    val context = RedisSerializationContext.newSerializationContext<String, Customer>(StringRedisSerializer()).apply {
        value(serializer)
    }.build()

    val customerRedis = ReactiveRedisTemplate<String, Customer>(factory, context)

    val customers = listOf(
            Customer(1, "Danny"),
            Customer(2, "Stephan"),
            Customer(3, "Thomas")
    )

    @Test
    fun writingAndReadingSingleValues() {

        customerRedis.connectionFactory.reactiveConnection.serverCommands().flushAll().block()

        val writing = Flux.fromIterable(customers).flatMap { customer: Customer ->
            customerRedis.opsForValue().set(customer.id.toString(), customer)
        }

        StepVerifier.create(writing)
                .expectNext(true, true, true)
                .verifyComplete()


        val keys = customerRedis.keys("*")

        StepVerifier.create(keys)
                .thenConsumeWhile { key -> customers.map { it.id.toString() }.contains(key) }
                .verifyComplete()

        StepVerifier.create(keys)
                .expectNextCount(3)
                .verifyComplete()

        val read = customerRedis.opsForValue()["1"]

        StepVerifier.create(read)
                .expectNext(Customer(1, "Danny"))
                .verifyComplete()

    }

    @Test
    fun wrtitingAndReadingListOfValues() {

        StepVerifier.create(customerRedis.opsForList().leftPushAll("customers", customers))
                .expectNext(3L)
                .verifyComplete()


        StepVerifier.create(customerRedis.opsForList().range("customers", 0, -1))
                .thenConsumeWhile { customer -> customers.contains(customer) }
                .verifyComplete()

        StepVerifier.create(customerRedis.opsForList().range("customers", 0, -1))
                .expectNextCount(3)
                .verifyComplete()

        customerRedis.expire("customers", Duration.ofMinutes(10))
    }

    @Test
    fun expireRemovesEntriesAfterSomeTime() {

        val writing = customerRedis.opsForValue().set("1", Customer(1, "Danny"))
                .flatMap { customerRedis.expire("1", Duration.ofSeconds(1)) }

        StepVerifier.create(writing).expectNext(true).verifyComplete()

        Thread.sleep(2000)

        val read = customerRedis.opsForValue()["1"]

        StepVerifier.create(read).expectNextCount(0).verifyComplete()

    }

//    @Test //TODO how the hell does scan work?
//    fun scanningCanFetchAllKeys() {
//
//        StepVerifier.create(Flux.fromIterable(customers).flatMap { customer: Customer ->
//            customerRedis.opsForValue().set(customer.id.toString(), customer)
//        }).verifyComplete()
//
//        StepVerifier.create(customerRedis.scan(ScanOptions.scanOptions().match("*").build()))
//                .expectNextCount(3)
//                .verifyComplete()
//
//    }

    companion object {
        val redisPort = 6381

        private val redisServer: RedisServer = RedisServer(redisPort)

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            redisServer.start()
        }

        @AfterClass
        @JvmStatic
        fun afterAll() {
            redisServer.stop()
        }

    }
}