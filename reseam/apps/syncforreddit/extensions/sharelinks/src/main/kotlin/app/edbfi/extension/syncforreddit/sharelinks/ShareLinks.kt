// SPDX-License-Identifier: AGPL-3.0-or-later
@file:JvmName("ShareLinks")

package app.edbfi.extension.syncforreddit.sharelinks

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import app.reseam.runtime.Export
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URI
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

private val redditHosts = setOf("reddit.com", "www.reddit.com", "old.reddit.com", "np.reddit.com", "m.reddit.com")
private val sharePath = Regex("/r/[A-Za-z0-9_]+/s/[A-Za-z0-9]+/?")
private val postPath = Regex("/(?:r/[A-Za-z0-9_]+/)?comments/[A-Za-z0-9]+(?:/.*)?")
private val executor = Executors.newSingleThreadExecutor()

@Export
external fun openResolved(context: Context, url: String)

@Export
fun resolve(context: Context?, link: String?): Boolean {
    if (context == null || link == null) return false
    val uri = try {
        URI(if (link.startsWith("/r/")) "https://www.reddit.com$link" else link)
    } catch (_: java.net.URISyntaxException) {
        return false
    }
    if (!isReddit(uri) || !sharePath.matches(uri.path.orEmpty())) return false

    val activity = WeakReference(context)
    val application = context.applicationContext
    executor.execute {
        val destination = followShareRedirects(URI("https://www.reddit.com${uri.path}"))
        Handler(Looper.getMainLooper()).post {
            val original = activity.get()
            val current = if (original is Activity && !original.isFinishing && !original.isDestroyed) {
                original
            } else {
                application
            }
            if (destination == null) {
                Toast.makeText(current, "Couldn't open this Reddit share link.", Toast.LENGTH_SHORT).show()
            } else {
                openResolved(current, destination)
            }
        }
    }
    return true
}

private fun isReddit(uri: URI): Boolean =
    (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) &&
        uri.host?.lowercase() in redditHosts && uri.port == -1 && uri.userInfo == null

private fun followShareRedirects(start: URI): String? {
    var uri = start
    repeat(5) {
        try {
            val connection = uri.toURL().openConnection() as HttpsURLConnection
            val location = try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                if (connection.responseCode !in listOf(301, 302, 303, 307, 308)) return null
                connection.getHeaderField("Location") ?: return null
            } finally {
                connection.disconnect()
            }
            val next = uri.resolve(location)
            if (!next.scheme.equals("https", true) || !isReddit(next)) return null
            if (postPath.matches(next.path.orEmpty())) return next.toASCIIString()
            uri = next
        } catch (_: IOException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
    }
    return null
}
