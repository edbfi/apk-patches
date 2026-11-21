// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.extension.rifisfun.reading

import okhttp3.HttpUrl

private val redditHosts = setOf("reddit.com", "www.reddit.com", "old.reddit.com", "new.reddit.com", "m.reddit.com")
private val postPath = Regex("""/(?:r/[A-Za-z0-9_]+/)?comments/[a-z0-9]+(?:/[A-Za-z0-9_-]+){0,2}/?""")

internal fun isRedditHost(host: String) = host in redditHosts

internal class ShareRedirects(start: HttpUrl) {
    // At most four requests, each with the client's own deadline.
    private val requested = mutableSetOf(start.withoutFragment())

    sealed interface Next {
        data class Request(val url: HttpUrl) : Next
        data class Post(val url: HttpUrl) : Next
    }

    fun follow(current: HttpUrl, status: Int, locations: List<String>): Next? {
        if (status !in setOf(301, 302, 303, 307, 308)) return null
        val target = locations.singleOrNull()?.let(current::resolve) ?: return null
        if (!target.isHttps || target.port != 443 || !isRedditHost(target.host) ||
            target.username.isNotEmpty() || target.password.isNotEmpty()) return null

        // Dispatch the exact post or comment target through rif, including its context parameters.
        if (postPath.matches(target.encodedPath)) return Next.Post(target)
        val next = target.withoutFragment()
        if (requested.size >= 4 || !requested.add(next)) return null
        return Next.Request(next)
    }

    private fun HttpUrl.withoutFragment() = newBuilder().fragment(null).build()
}
