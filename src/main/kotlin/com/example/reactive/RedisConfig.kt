package com.example.reactive

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheManager
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
class RedisConfig {

    @Bean
    @Primary
    fun connectionFactory(): ReactiveRedisConnectionFactory {
        return LettuceConnectionFactory("localhost", 6380)
    }

    @Bean
    fun redisOperations(factory: ReactiveRedisConnectionFactory): ReactiveRedisOperations<String, Customer> {
        val serializer: Jackson2JsonRedisSerializer<Customer> = Jackson2JsonRedisSerializer<Customer>(Customer::class.java)

        serializer.setObjectMapper(jacksonObjectMapper())

        val builder: RedisSerializationContextBuilder<String, Customer> = RedisSerializationContext.newSerializationContext<String, Customer>(StringRedisSerializer())
        val context: RedisSerializationContext<String, Customer> = builder.value(serializer).build()
        return ReactiveRedisTemplate<String, Customer>(factory, context)
    }

}