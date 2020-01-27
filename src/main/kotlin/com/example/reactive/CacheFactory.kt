package com.example.reactive

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.stereotype.Component
import reactor.cache.CacheFlux
import reactor.cache.CacheMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import java.time.Duration


@Component
class CacheFactory(val factory: ReactiveRedisConnectionFactory) {

    fun <T> getCacheProvider(clazz: Class<T>): ReactiveCacheProvider<T> {
        val serializer: Jackson2JsonRedisSerializer<T> = Jackson2JsonRedisSerializer<T>(clazz)

        serializer.setObjectMapper(jacksonObjectMapper())

        val builder: RedisSerializationContext.RedisSerializationContextBuilder<String, T> = RedisSerializationContext.newSerializationContext<String, T>(StringRedisSerializer())
        val context: RedisSerializationContext<String, T> = builder.value(serializer).build()

        return ReactiveCacheProvider(ReactiveRedisTemplate<String, T>(factory, context))
    }

}

class ReactiveCacheProvider<T>(val redis: ReactiveRedisOperations<String, T>) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val keyTimeToLive = Duration.ofMinutes(5)


    fun forValue(key: String, producer: () -> T?): Mono<T> =
            CacheMono.lookup(this::readRedisValue, key)
                    .onCacheMissResume { Mono.fromCallable(producer) }
                    .andWriteWith { k, v -> writeRedisValue(k, v) }

    fun forCollection(key: String, producer: () -> List<T>): Flux<T> =
        CacheFlux.lookup(this::readRedisList, key)
            .onCacheMissResume( Mono.fromCallable(producer).flatMapMany { Flux.fromIterable(it) } )
            .andWriteWith { k, v -> writeRedisList(k, v) }



    private fun readRedisValue(key: String) =
            redis.opsForValue()[key].map {
                logger.info("Cache result: $it")
                Signal.next(it) as Signal<out T>
            }

    private fun writeRedisValue(key: String, value: Signal<out T>) =
            value.get()?.let {
                logger.info("Writing to cache: $it - with key $key")
                redis.opsForValue().set(key, it).flatMap {
                    redis.expire(key, keyTimeToLive)
                }.then()
            }

    private fun readRedisList(key: String) = redis.hasKey(key).flatMap {
        when(it) {
            true -> redis.opsForList().range(key, 0, -1).materialize().collectList()
            false -> Mono.empty()
        }
    }


    private fun writeRedisList(key: String, values : List<Signal<T>>) =
            redis.opsForList().leftPushAll(key, values.filter { it.hasValue() }.map { it.get()!! }.toList())
            .flatMap { redis.expire(key, keyTimeToLive) }.then()


}