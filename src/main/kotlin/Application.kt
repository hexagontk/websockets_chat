package org.example

import com.hexagonkt.core.ALL_INTERFACES
import com.hexagonkt.core.logging.LoggingManager
import com.hexagonkt.core.logging.logger
import com.hexagonkt.core.media.TEXT_PLAIN
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.Header
import com.hexagonkt.http.model.ws.WsSession
import com.hexagonkt.http.server.HttpServer
import com.hexagonkt.http.server.HttpServerSettings
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.http.server.serve
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal val settings = HttpServerSettings(ALL_INTERFACES, 9090)
internal val serverAdapter = JettyServletAdapter(minThreads = 4)

internal lateinit var server: HttpServer
internal val clientSessions = hashMapOf<WsSession, String>()

internal fun main() {
    LoggingManager.defaultLoggerName = "org.hexagon.ws-chat"
    var userNumber = 1

    server = serve(serverAdapter, settings) {
        on("*") {
            send(headers = response.headers + Header("server", "Hexagon/2.8"))
        }

        get("/text") {
            ok("Hello, World!", contentType = ContentType(TEXT_PLAIN))
        }

        ws("/chat") {
            logger.info { "/chat" }
            accepted(
                onConnect = {
                    val name = userNumber++.toString()
                    clientSessions[this] = name
                    logger.info { "onConnect: $name" }
                    broadcastMsg("Server", "$name joined the chat.")
                },
                onClose = {status, reason ->
                    val name = clientSessions.remove(this)
                    logger.info { "onClose: $name, ($status: $reason)" }
                    broadcastMsg("Server", "$name left the chat.")
                },
                onText = {text ->
                    val name = clientSessions[this]
                    logger.info { "onText: $name, $text" }
                    broadcastMsg(name, text)
                }
            )
        }
    }
}

val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
fun broadcastMsg(sender: String?, msg: String) {
    val time = LocalDateTime.now().format(formatter)
    val message = "$time $sender: $msg"

    val data = mapOf(
        "message" to message,
        "userList" to clientSessions.values
    ).toString().toByteArray()

    clientSessions.forEach { (session, _) ->
        session.send(data)
    }
}
