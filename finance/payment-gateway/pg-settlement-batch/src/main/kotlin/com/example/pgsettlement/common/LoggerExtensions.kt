package com.example.pgsettlement.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T : Any> T.logger(): Lazy<Logger> =
    lazy {
        LoggerFactory.getLogger(T::class.java)
    }
