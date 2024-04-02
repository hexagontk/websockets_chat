package org.example

import com.google.gson.Gson
import com.hexagonkt.core.ALL_INTERFACES
import com.hexagonkt.core.logging.logger
import com.hexagonkt.core.media.TEXT_PLAIN
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.Header
import com.hexagonkt.http.model.ws.WsSession
import com.hexagonkt.http.server.HttpServer
import com.hexagonkt.http.server.HttpServerSettings
import com.hexagonkt.http.server.netty.serve

internal val settings = HttpServerSettings(ALL_INTERFACES, 9090)

internal lateinit var server: HttpServer
internal val clientSessions = hashMapOf<WsSession, String>()

internal fun main() {
    var userNumber = 1

    server = serve(settings) {
        after("*") {
            send(headers = response.headers + Header("server", "Hexagon/3.5"))
        }

        get("/text") {
            ok("Hello, World!", contentType = ContentType(TEXT_PLAIN))
        }

        ws("/chat/{name}") {
            val nameParam = pathParameters["name"]
            logger.info { "/chat" }
            accepted(
                onConnect = {
                    val name = nameParam ?: userNumber++.toString()
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

val gson = Gson()
fun broadcastMsg(sender: String?, msg: String) {
    val message = "$sender: $msg"
    val data = mapOf(
        "message" to message,
        "userList" to clientSessions.values
    )
    val dataJson = gson.toJson(data)

    clientSessions.forEach { (session, _) ->
        session.send(dataJson)
    }
}
