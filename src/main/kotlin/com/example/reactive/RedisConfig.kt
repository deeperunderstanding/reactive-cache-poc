package com.example.reactive

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializationContext.RedisSerializationContextBuilder
import org.springframework.data.redis.serializer.StringRedisSerializer


@Configuration
class CoffeeConfiguration {

    @Bean
    @Primary
    fun connectionFactory(): ReactiveRedisConnectionFactory {
        return LettuceConnectionFactory("localhost", 6380)
    }

    @Bean
    fun redisOperations(factory: ReactiveRedisConnectionFactory): ReactiveRedisOperations<String, Coffee> {
        val serializer: Jackson2JsonRedisSerializer<Coffee> = Jackson2JsonRedisSerializer<Coffee>(Coffee::class.java)

        serializer.setObjectMapper(jacksonObjectMapper())

        val builder: RedisSerializationContextBuilder<String, Coffee> = RedisSerializationContext.newSerializationContext<String, Coffee>(StringRedisSerializer())
        val context: RedisSerializationContext<String, Coffee> = builder.value(serializer).build()
        return ReactiveRedisTemplate<String, Coffee>(factory, context)
    }


//    @Bean
//    @Primary
//    final inline fun <K, reified V> genericRedisOperations(factory: ReactiveRedisConnectionFactory): ReactiveRedisOperations<K, V> {
//        val serializer: Jackson2JsonRedisSerializer<V> = Jackson2JsonRedisSerializer<V>(V::class.java)
//
//        serializer.setObjectMapper(jacksonObjectMapper())
//
//        val builder: RedisSerializationContextBuilder<K, V> = RedisSerializationContext.newSerializationContext<K, V>(StringRedisSerializer())
//        val context: RedisSerializationContext<K, V> = builder.value(serializer).build()
//        return ReactiveRedisTemplate<K, V>(factory, context)
//    }
}