// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.patches.syncforreddit.misc.ultra

import app.edbfi.extension.syncforreddit.websitepreview.bindings.WebsitePreviews
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.points
import app.reseam.patch.redirectTo

private const val CONTEXT = "android.content.Context"
private const val GLIDE = "com.bumptech.glide.b"
private const val REQUEST_MANAGER = "com.bumptech.glide.g"
private const val REQUEST_BUILDER = "com.bumptech.glide.f"
private const val SITE_PREVIEW_SETTING = "ultra_website_previews"

private val previewHelper = klass("Website preview helper") {
    sourceFile("WebsitePreviewHelper.java")
}

private val websiteEligibility = methods("Check stock website preview eligibility") {
    inClass(previewHelper)
    params(Type.String)
    returns(Type.Boolean)
    flags(AccessFlags.STATIC)
    accesses { name(SITE_PREVIEW_SETTING) }
}.single()

private val stockImageUrl = methods("Build the stock website preview URL") {
    inClass(previewHelper)
    params(Type.String)
    returns(Type.String)
    strings("https://ap.syncforreddit.com/image?url=", "https://i.ytimg.com/vi/", "/amp/s/")
}.single()

private val linkTypeHelper = klass("Link type helper") {
    sourceFile("LinkTypeHelper.java")
}

private val isYouTube = methods("Recognize a YouTube video link") {
    inClass(linkTypeHelper)
    params(Type.String)
    returns(Type.Boolean)
    flags(AccessFlags.STATIC)
    strings("/shorts/", "youtube.com", "youtu.be")
    calledBy(stockImageUrl)
}.single()

private val redditApplication = klass("com.laurencedawson.reddit_sync.RedditApplication")
private val appContext = methods("Get Sync application context") {
    inClass(redditApplication)
    flags(AccessFlags.STATIC or AccessFlags.PUBLIC)
    params()
    returns(CONTEXT)
}.single()

private val glideModule = klass("com.laurencedawson.reddit_sync.YourAppGlideModule")
private val registerModels = methods("Register Sync's Glide models") {
    inClass(glideModule)
    param(0, CONTEXT)
    param(1, GLIDE)
    paramCount(3)
    returns(Type.Void)
    strings("Setting up glide with OkHTTP")
}.single()

private val inlineLoader = methods("Bind comment link spans") {
    inClass(klass("Spannable text builder") {
        sourceFile("SpannableTextViewStringBuilder.java")
        hasInstanceField("java.lang.StringBuilder")
    })
    returns(Type.Void)
    strings("EfficientImageSpan", "tenor.com")
}.single()

private val spanModel = methods("Read the inline website image model") {
    inClass(klass("Website preview span") { sourceFile("WebPreviewImageSpan.java") })
    flags(AccessFlags.PUBLIC)
    params()
    returns(Type.String)
    opcodeSequence(Opcode.IGET_OBJECT, Opcode.RETURN_OBJECT)
    calledBy(inlineLoader)
}.single()

private val sheet = klass("Link options sheet") {
    sourceFile("UrlSelectionDialogBottomSheet.java")
    strings("Link options", "Open in browser", "Retrying")
}

private val sheetLoad = methods("Load a website image in the link options sheet") {
    inClass(sheet)
    flags(AccessFlags.PRIVATE)
    params()
    returns(Type.Void)
    calls(stockImageUrl)
}.single()

private val sheetFirstLoad = methods("Build the link options sheet") {
    inClass(sheet)
    strings("Open in browser", "Copy link address")
    calls(websiteEligibility)
}.single().points("Start sheet website preview") { calls(sheetLoad) }.single()

private val sheetRetry = methods("Retry a link options preview") {
    inClass(sheet)
    params("android.view.View")
    strings("Retrying")
}.single().points("Retry sheet website preview") { calls(sheetLoad) }.single()

private val inlineImage = inlineLoader.points("Load the website image span") {
    calls(spanModel)
    then { opcode(Opcode.MOVE_RESULT_OBJECT) }
    then {
        invokeVirtual {
            owner(REQUEST_MANAGER)
            params(Type.String)
            returns(REQUEST_BUILDER)
        }
    }
}.single()

private val sheetImage = sheetLoad.points("Load the link sheet website image") {
    calls(stockImageUrl)
    then { opcode(Opcode.MOVE_RESULT_OBJECT) }
    then {
        invokeVirtual {
            owner(REQUEST_MANAGER)
            params(Type.String)
            returns(REQUEST_BUILDER)
        }
    }
}.single()

internal fun resolveWebsitePreviewEdits(): () -> Unit {
    listOf(websiteEligibility, isYouTube, appContext, registerModels).forEach { it.method }
    // Both sheet entry points must still reach the image loader we edit.
    listOf(inlineImage, sheetImage, sheetFirstLoad, sheetRetry).forEach { it.index }
    check(registerModels.method.parameterTypes.last() == "Lw2/e;") { "Unexpected Glide registry type" }

    return {
        WebsitePreviews.stockEligibility.implement { returnValue(call(websiteEligibility, param(0))) }
        websiteEligibility.after {
            whenTrue(result) {
                whenFalse(call(isYouTube, param(0))) {
                    result.assign(WebsitePreviews.directAvailable(call(appContext)))
                }
            }
        }
        registerModels.after { WebsitePreviews.register(param(0), param(2)) }
        // Leave the stock proxy strings in the span constructor and layout callback for sizing.
        // The Tenor branch of the inline renderer has its own request and stock URL helper call.
        inlineImage.redirectTo(WebsitePreviews.load)
        sheetImage.redirectTo(WebsitePreviews.load)
    }
}
