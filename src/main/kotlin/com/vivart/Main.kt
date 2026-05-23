package com.vivart

import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

fun main(): Unit = runBlocking {
    KotlinLoggingConfiguration.logStartupMessage = false
    val server = buildServer()

    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered(),
    )

    val done = CompletableDeferred<Unit>()
    server.onClose { done.complete(Unit) }
    server.createSession(transport)
    done.await()
}
