package com.example.reactive

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.stereotype.Component
import reactor.cache.CacheMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import javax.annotation.PostConstruct

@Component
class CustomerService(
        val customerRepo : CustomerRepository,
        private val factory: ReactiveRedisConnectionFactory,
        private val customerOps: ReactiveRedisOperations<String, Customer>
) {

    val logger = LoggerFactory.getLogger(this::class.java)

    //@PostConstruct
    fun preLoadCache() {
        logger.info("Loading Customers into Cache")
        factory.reactiveConnection.serverCommands().flushAll().thenMany(
                Flux.just(Customer(1, "Danny"), Customer(2, "Stephan"), Customer(3, "Thomas"))
                        .flatMap { customer: Customer ->
                            logger.info("storing customer $customer in Redis")
                            customerOps.opsForValue().set(cacheKey("customer", customer.id.toString()), customer)
                        })
                .thenMany(customerOps.keys("*") //TODO apparently keys() is a bad way of reading keys
                        .flatMap { key: String -> customerOps.opsForValue()[key] })
                .doOnError { logger.error("Error when reading cache", it) }

                .subscribe { x: Customer -> logger.info(x.toString()) }
    }

    fun customerById(id: Long): Mono<Customer> = CacheMono
            .lookup(this::readRedisCache, cacheKey("customer", id.toString()))
            .onCacheMissResume { Mono.from { customerRepo.findById(id) } }
            .andWriteWith { k, v -> writeToRedisCache(k, v) }


    fun cacheKey(prefix: String, id: String) = "$prefix:$id"

    fun readRedisCache(key: String) = customerOps.opsForValue()[key].map {
        logger.info("Cache result: $it")
        Signal.next(it) as Signal<out Customer>
    }

    fun writeToRedisCache(key: String, value: Signal<out Customer>) =
            value.get()?.let {
                logger.info("Writing to cache: $it - with key $key")
                customerOps.opsForValue().set(key, it).then()
            }


}