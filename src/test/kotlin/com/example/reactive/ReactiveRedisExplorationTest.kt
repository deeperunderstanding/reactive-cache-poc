package com.example.reactive

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import redis.embedded.RedisServer

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

    @Test
    fun writingAndReadingReactiveRedis() {

        val customers = listOf(
                Customer(1, "Danny"),
                Customer(2, "Stephan"),
                Customer(3, "Thomas")
        )

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