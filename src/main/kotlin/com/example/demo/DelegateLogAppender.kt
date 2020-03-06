package com.example.demo

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.ext.spring.ApplicationContextHolder
import ch.qos.logback.ext.spring.EventCacheMode
import ch.qos.logback.ext.spring.ILoggingEventCache
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import java.lang.Enum

class DelegateLogAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
    private val lock: Any
    var beanName: String? = null
    private var cache: ILoggingEventCache? = null
    private var cacheMode: EventCacheMode

    @Volatile
    private var delegate: Appender<ILoggingEvent>? = null
    fun setCacheMode(mode: String) {
        cacheMode = Enum.valueOf(EventCacheMode::class.java, mode.toUpperCase())
    }

    override fun start() {
        if (isStarted) {
            return
        }
        if (beanName == null || beanName!!.trim { it <= ' ' }.isEmpty()) {
            check(!(name == null || name.trim { it <= ' ' }
                .isEmpty())) { "A 'name' or 'beanName' is required for DelegatingLogbackAppender" }
            beanName = name
        }
        cache = cacheMode.createCache()
        super.start()
    }

    override fun stop() {
        super.stop()
        if (cache != null) {
            cache = null
        }
        if (delegate != null) {
            delegate!!.stop()
            delegate = null
        }
    }

    override fun append(event: ILoggingEvent) {
        //Double-check locking here to optimize out the synchronization after the delegate is in place. This also has
        //the benefit of dealing with the race condition where 2 threads are trying to log and one gets the lock with
        //the other waiting and the lead thread sets the delegate, logs all cached events and then returns, allowing
        //the blocked thread to acquire the lock. At that time, the delegate is no longer null and the event is logged
        //directly to it, rather than being cached.
        if (delegate == null) {
            synchronized(lock) {

                //Note the isStarted() check here. If multiple threads are logging at the time the ApplicationContext
                //becomes available, the first thread to acquire the lock _may_ stop this appender if the context does
                //not contain an Appender with the expected name. If that happens, when the lock is released and other
                //threads acquire it, isStarted() will return false and those threads should return without trying to
                //use either the delegate or the cache--both of which will be null.
                if (!isStarted) {
                    return
                }
                //If we're still started either no thread has attempted to load the delegate yet, or the delegate has
                //been loaded successfully. If the latter, the delegate will no longer be null
                if (delegate == null) {
                    if (ApplicationContextHolder.hasApplicationContext()) {
                        //First, load the delegate Appender from the ApplicationContext. If it cannot be loaded, this
                        //appender will be stopped and null will be returned.
                        val appender = getDelegate() ?: return

                        //Once we have the appender, unload the cache to it.
                        val cachedEvents = cache!!.get()
                        for (cachedEvent in cachedEvents) {
                            appender.doAppend(cachedEvent)
                        }

                        //If we've found our delegate appender, we no longer need the cache.
                        cache = null
                        delegate = appender
                    } else {
                        //Otherwise, if the ApplicationContext is not ready yet, cache this event and wait
                        cache!!.put(event)

                        // TODO this is new
                        println(event)

                        return
                    }
                }
            }
        }

        //If we make it here, the delegate should always be non-null and safe to append to.
        delegate!!.doAppend(event)
    }

    private fun getDelegate(): Appender<ILoggingEvent>? {
        val context = ApplicationContextHolder.getApplicationContext()
        try {
            val appender =
                context.getBean(
                    beanName!!,
                    Appender::class.java
                ) as Appender<ILoggingEvent>
            appender.context = getContext()
            if (!appender.isStarted) {
                appender.start()
            }
            return appender
        } catch (e: NoSuchBeanDefinitionException) {
            stop()
            addError(
                "The ApplicationContext does not contain an Appender named [" + beanName +
                        "]. This delegating appender will now stop processing events.", e
            )
        }
        return null
    }

    init {
        cacheMode = EventCacheMode.ON
        lock = Any()
    }
}
