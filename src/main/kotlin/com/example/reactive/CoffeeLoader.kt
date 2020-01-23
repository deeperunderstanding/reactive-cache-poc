package com.example.reactive

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.util.*
import javax.annotation.PostConstruct


@Component
class CoffeeLoader(private val factory: ReactiveRedisConnectionFactory, private val coffeeOps: ReactiveRedisOperations<String, Coffee>) {

    val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun loadData() {
        logger.info("Loading Coffee into Cache")
        factory.reactiveConnection.serverCommands().flushAll().thenMany(
                Flux.just("Jet Black Redis", "Darth Redis", "Black Alert Redis")
                        .map { name: String -> Coffee(name) }
                        .flatMap { coffee: Coffee ->
                            logger.info("storing coffe $coffee in Redis")
                            coffeeOps.opsForValue().set(coffee.name, coffee)
                        })
                .thenMany(coffeeOps.keys("*")
                        .flatMap { key: String -> coffeeOps.opsForValue()[key] })
                .doOnError { logger.error("Error when reading chache", it) }

                .subscribe { x: Coffee -> logger.info(x.toString()) }
    }

}