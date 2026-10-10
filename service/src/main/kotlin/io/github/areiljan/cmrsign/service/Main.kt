package io.github.areiljan.cmrsign.service

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.executor = Executors.newVirtualThreadPerTaskExecutor()
    server.createContext("/health") { exchange ->
        val body = "ok".toByteArray()
        exchange.sendResponseHeaders(200, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }
    server.start()
    println("cmr-sign service listening on :$port")
}
