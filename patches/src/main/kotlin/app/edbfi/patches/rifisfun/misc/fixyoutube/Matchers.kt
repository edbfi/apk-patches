// SPDX-License-Identifier: AGPL-3.0-or-later
package app.edbfi.patches.rifisfun.misc.fixyoutube

import app.edbfi.patches.shared.uniqueMethod
import app.revanced.patcher.custom
import app.revanced.patcher.definingClass
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.name
import app.revanced.patcher.parameterTypes
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.returnType

internal const val APP_ORIGIN = "https://www.youtube.com"

/** The IFrame player's WebView, from android-youtube-player. R8 kept its name: the app's layouts name it. */
internal const val PLAYER_WEBVIEW = "Lcom/pierfrancescosoffritti/androidyoutubeplayer/core/player/views/WebViewYouTubePlayer;"

// R8 renamed the library's IFramePlayerOptions and its Builder, so these match on the player vars and signatures.

/** The options builder's constructor puts the default player vars, `origin` among them. */
internal fun BytecodePatchContext.optionsBuilderMethod() = uniqueMethod("the IFrame player options builder", "origin", APP_ORIGIN, "enablejsapi", "iv_load_policy") {
    name("<init>")
    returnType("V")
    parameterTypes()
}

/** The options' origin getter, which reads `origin` back. */
internal fun BytecodePatchContext.originGetterMethod(options: String) = uniqueMethod("the IFrame player options' origin getter", "origin") {
    definingClass(options)
    returnType("Ljava/lang/String;")
    parameterTypes()
}

/** The WebView's setup loads the player page with the origin as its base URL. */
internal fun BytecodePatchContext.loadPlayerMethod(originGetter: String) = uniqueMethod("the IFrame player page load") {
    definingClass(PLAYER_WEBVIEW)
    custom {
        val calls = implementation?.instructions?.mapNotNull { it.methodReference }.orEmpty()
        calls.any { "${it.definingClass}->${it.name}" == originGetter } &&
            calls.any { it.definingClass == "Landroid/webkit/WebView;" && it.name == "loadDataWithBaseURL" }
    }
}
