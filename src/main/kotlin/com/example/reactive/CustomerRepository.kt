package com.example.reactive

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Fake Customer Repository to demonstrate Caching
 */
@Component
class CustomerRepository {

    val logger = LoggerFactory.getLogger(this::class.java)

    private val customers = mapOf(
            1L to Customer(1, "Danny"),
            2L to Customer(2, "Stephan"),
            3L to Customer(3, "Thomas")
    )

    fun findById(id: Long): Customer? {
        logger.info("Customer repository was called with $id")
        Thread.sleep(3000) //its a BIG production table
        return customers[id]
    }

}
