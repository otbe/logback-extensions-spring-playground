package com.example.demo

import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

//@Component
class FooService {
    fun test() {
        logger.info("bar")
    }
}