package com.missaolive.app

import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class MjpegServer(port: Int) : NanoHTTPD(port) {
    private val latest = AtomicReference<ByteArray?>(null)
    fun publish(bytes: ByteArray) { latest.set(bytes) }
    override fun serve(session: IHTTPSession): Response {
        if (session.uri != "/video.mjpg") return newFixedLengthResponse(Response.Status.OK, "text/html", "Missãolive MJPEG receiver. Use /video.mjpg")
        val stream = object : java.io.InputStream() {
            private var pending = ByteArray(0); private var index = 0
            override fun read(): Int {
                while (index >= pending.size) {
                    val frame = latest.get()
                    if (frame == null) { try { Thread.sleep(50) } catch (_: InterruptedException) {}; continue }
                    pending = ("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.size}\r\n\r\n".toByteArray() + frame + "\r\n".toByteArray())
                    index = 0
                }
                return pending[index++].toInt() and 0xff
            }
        }
        return newChunkedResponse(Response.Status.OK, "multipart/x-mixed-replace; boundary=frame", stream)
    }
}
