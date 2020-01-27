package com.example.reactive

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class CustomerController(
        val customerService: CustomerService
) {

    @GetMapping("/customer/{id}")
    fun getCustomerById(@PathVariable("id") id: Long) = customerService.customerById(id)

    @GetMapping("/customer")
    fun getCustomerById() = customerService.customers()

}