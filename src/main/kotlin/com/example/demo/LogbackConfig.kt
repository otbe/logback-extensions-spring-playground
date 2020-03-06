package com.example.demo

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.ext.spring.ApplicationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class LogbackConfig {

    @Bean
    fun applicationContextHolder(): ApplicationContextHolder {
        return ApplicationContextHolder()
    }

    @Bean
    fun loggerContext(): LoggerContext {
        return LoggerFactory.getILoggerFactory() as LoggerContext
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun encoder(ctx: LoggerContext): PatternLayoutEncoder {
        val encoder = PatternLayoutEncoder()
        encoder.context = ctx
        encoder.pattern = "%d{dd-MM-yyyy HH:mm:ss.SSS} %magenta([%thread]) %highlight(%-5level) %logger{36}.%M - %msg%n"
        return encoder
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun consoleAppender(ctx: LoggerContext, encoder: PatternLayoutEncoder): ConsoleAppender<Any> {
        val appender = ConsoleAppender<Any>()
        appender.context = ctx
        appender.encoder = encoder as Encoder<Any>
        return appender
    }
}