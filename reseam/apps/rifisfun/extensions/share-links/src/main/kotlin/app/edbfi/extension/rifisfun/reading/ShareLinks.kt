// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShareLinks")

package app.edbfi.extension.rifisfun.reading

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import app.reseam.runtime.Export
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

private val sharePath = Regex("""/r/[A-Za-z0-9_]+/s/[A-Za-z0-9]+/?""")
private val main = Handler(Looper.getMainLooper())

private fun InetAddress.isPublic(): Boolean {
    if (this !is Inet4Address || isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress ||
        isSiteLocalAddress || isMulticastAddress) return false
    val first = address[0].toInt() and 255
    val second = address[1].toInt() and 255
    val third = address[2].toInt() and 255
    return first in 1..223 &&
        !(first == 100 && second in 64..127) &&
        !(first == 192 && (second == 0 || second == 88 && third == 99)) &&
        !(first == 198 && (second in 18..19 || second == 51 && third == 100)) &&
        !(first == 203 && second == 0 && third == 113)
}

private val client by lazy {
    OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (!isRedditHost(hostname)) throw UnknownHostException(hostname)
                return Dns.SYSTEM.lookup(hostname).filter { it.isPublic() }
                    .ifEmpty { throw UnknownHostException(hostname) }
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .callTimeout(4, TimeUnit.SECONDS)
        .build()
}

private fun HttpUrl.clean() = newBuilder().query(null).fragment(null).build()

private fun String.toShareUrl(): HttpUrl? {
    val url = toHttpUrlOrNull() ?: return null
    if (!isRedditHost(url.host) || url.port != (if (url.isHttps) 443 else 80)) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty() || !sharePath.matches(url.encodedPath)) return null
    return url.clean().newBuilder().scheme("https").build()
}

private fun resolve(url: HttpUrl, finish: (String?) -> Unit) {
    val redirects = ShareRedirects(url)

    fun request(current: HttpUrl) {
        client.newCall(Request.Builder().url(current).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) = finish(null)

            override fun onResponse(call: Call, response: Response) {
                val next = response.use { redirects.follow(current, it.code, it.headers("Location")) }
                when (next) {
                    is ShareRedirects.Next.Request -> request(next.url)
                    is ShareRedirects.Next.Post -> finish(next.url.toString())
                    null -> finish(null)
                }
            }
        })
    }

    request(url)
}

private fun showFailure(context: Context) {
    Toast.makeText(context, "Could not resolve Reddit share link.", Toast.LENGTH_SHORT).show()
}

@Export
fun resolveIncoming(activity: Activity): Boolean {
    val incoming = activity.intent ?: return false
    if (incoming.action != Intent.ACTION_VIEW && incoming.action != "android.nfc.action.NDEF_DISCOVERED") return false
    val url = incoming.dataString?.toShareUrl() ?: return false
    resolve(url) { destination ->
        main.post {
            if (!activity.isFinishing && !activity.isDestroyed) {
                val follow = if (destination == null) {
                    showFailure(activity)
                    Intent(Intent.ACTION_MAIN)
                } else {
                    Intent(incoming).setData(android.net.Uri.parse(destination))
                }.setClass(activity, activity.javaClass)
                activity.startActivity(follow)
                activity.finish()
            }
        }
    }
    return true
}

@Export
fun resolveInApp(
    raw: String?, display: String?, mediaType: Any?, thread: String?, title: String?,
    nsfw: Boolean, whitelist: Any?, fragments: Any?, context: Context?, source: Any?,
): Boolean {
    val url = raw?.toShareUrl() ?: return false
    resolve(url) { destination ->
        main.post {
            if (context is Activity && (context.isFinishing || context.isDestroyed)) return@post
            if (destination != null) {
                resumeInApp(destination, display, mediaType, thread, title, nsfw,
                    whitelist, fragments, context, source)
            } else if (context != null) {
                showFailure(context)
            }
        }
    }
    return true
}

@Export
external fun resumeInApp(
    raw: String, display: String?, mediaType: Any?, thread: String?, title: String?,
    nsfw: Boolean, whitelist: Any?, fragments: Any?, context: Context?, source: Any?,
)
