package com.example.reactive

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomerService(
        val customerRepo: CustomerRepository,
        cacheFactory: CacheFactory
) {

    val customerCache = cacheFactory.getCacheProvider(Customer::class.java)

    fun customerById(id: Long) = customerCache.forValue("customer:$id") {
        customerRepo.findById(id)
    }

    fun customers() = customerCache.forCollection("customers") {
        customerRepo.findAll()
    }

}



