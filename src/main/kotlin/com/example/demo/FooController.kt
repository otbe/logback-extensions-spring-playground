package com.example.demo

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController


@RestController
class FooController(private val fooService: FooService) {
    @RequestMapping("/", method = [RequestMethod.GET])
    fun getMe(): String {
        fooService.test()
        return "foo"
    }
}