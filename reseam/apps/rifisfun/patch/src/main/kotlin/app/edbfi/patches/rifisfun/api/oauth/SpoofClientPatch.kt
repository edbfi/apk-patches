// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.patches.rifisfun.api.oauth

import app.edbfi.extension.shared.oauth.bindings.OAuthRedirect
import app.edbfi.patches.rifisfun.RifPatchStrings
import app.edbfi.patches.rifisfun.rifIsFun
import app.edbfi.patches.shared.oauth.OAuthConfig
import app.reseam.patch.MethodTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.alwaysReturn
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.methodRef
import app.reseam.patch.dex.stringValue
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.redirectTo
import app.reseam.patch.replace

private const val STOCK_REDIRECT = "redditisfun://auth"
private const val LOGIN = "com.andrewshu.android.reddit.login.oauth2.OAuth2Activity"
private const val URI = "android.net.Uri"
private const val URI_BUILDER = "android.net.Uri\$Builder"

private val authorize = method("rif Reddit authorization URL") {
    params()
    returns(URI)
    strings("https://old.reddit.com/api/v1/authorize.compact", "client_id", STOCK_REDIRECT, "scope")
}

private val authorizeClient = authorize.points("authorization client ID query parameter") {
    invokeVirtual { owner(URI_BUILDER); name("appendQueryParameter"); params(Type.String, Type.String) }
    argument(1) { string("client_id") }
}.single().captureArgumentAs("clientId", 2)

private fun redirectIn(target: MethodTarget) = target.points("rif OAuth redirect literal") {
    string(STOCK_REDIRECT)
}.single().captureAs("redirect", Type.String)

private val codeExchange = method("rif authorization code form") {
    params()
    returns("[Landroidx/core/util/c;")
    strings("authorization_code", "redirect_uri", STOCK_REDIRECT)
}

private val webCallback = method("rif login WebView callback") {
    name("shouldOverrideUrlLoading")
    params("android.webkit.WebView", Type.String)
    returns(Type.Boolean)
    strings(STOCK_REDIRECT, "state", "code", "error")
}

private val callbackPredicate = webCallback.points("rif WebView callback prefix check") {
    invokeVirtual { owner("java.lang.String"); name("startsWith"); params(Type.String); returns(Type.Boolean) }
    argument(1) { string(STOCK_REDIRECT) }
}.single()

private val stateCheckCall = webCallback.points("rif native OAuth state comparison") {
    invokeVirtual { returns(Type.Boolean); params(Type.String) }
    where { methodRef?.definingClass == authorize.owner }
}.single()

private val stateCheck = stateCheckCall.callee()

private val loginSession = webCallback.points("rif OAuth login session") {
    invokeStatic { owner(authorize.owner); params(); returns(authorize.owner) }
}.single().callee()

private val tokenClassInit = method("rif Reddit token request base class initializer") {
    name("<clinit>")
    strings("https://www.reddit.com/api/v1/access_token")
}

private val authHeader = method("rif Reddit token Authorization header") {
    flags(AccessFlags.PROTECTED)
    paramCount(4)
    param(2, Type.String)
    param(3, URI)
    returns(Type.Void)
    strings("Authorization", "Basic ")
}

private val authHeaderSetter = authHeader.points("rif OAuth Authorization header setter") {
    invokeVirtual { params(Type.String, Type.String) }
    argument(1) { string("Authorization") }
}.single().callee()

private val tokenForms = methods("rif guest and refresh token forms") {
    params()
    returns("[Landroidx/core/util/c;")
    strings("grant_type")
}

private val webUserAgent = method("rif OAuth WebView settings") {
    inClass(klass(LOGIN))
    params()
    returns(Type.Void)
    calls { owner("android.webkit.WebSettings"); name("setUserAgentString"); params(Type.String) }
}

private val userAgent = webUserAgent.points("rif shared Reddit User-Agent provider") {
    invokeStatic { returns(Type.String); params() }
}.single().callee()

private val requestUserAgent = methods("rif Reddit request User-Agent header") {
    params(authHeader.parameterTypes[1])
    returns(Type.Void)
    strings("User-Agent")
    calls(userAgent)
}.single {
    authHeader.method.classDef.superclassChain.any { it.descriptor == owner }
}

val spoofClient = patch(RifPatchStrings.spoofClient) {
    description(RifPatchStrings.spoofClientDescription)
    compatibleWith(rifIsFun)

    val clientId = stringOption("clientId", RifPatchStrings.oauthClientId, RifPatchStrings.oauthClientIdDescription)
    val redirectUri = stringOption("redirectUri", RifPatchStrings.oauthRedirectUri, RifPatchStrings.oauthRedirectUriDescription)
    val agent = stringOption("userAgent", RifPatchStrings.oauthUserAgent, RifPatchStrings.oauthUserAgentDescription)

    execute {
        val config = OAuthConfig.read(options.getOrNull(clientId), options.getOrNull(redirectUri),
            options.getOrNull(agent)) ?: return@execute

        val authorizeRedirect = redirectIn(authorize)
        val exchangeRedirect = redirectIn(codeExchange)
        val callbackRedirect = redirectIn(webCallback)
        authorizeClient.instruction
        authorizeRedirect.instruction
        exchangeRedirect.instruction
        callbackRedirect.instruction
        callbackPredicate.instruction
        stateCheck.method
        loginSession.method
        check(authHeader.owner == tokenClassInit.owner) { "Unexpected rif OAuth token header provider." }
        authHeaderSetter.method
        val forms = tokenForms.all.filter {
            it.method.instructions.any { insn ->
                insn.stringValue in setOf("authorization_code",
                    "https://oauth.reddit.com/grants/installed_client", "refresh_token")
            }
        }
        check(forms.size == 3 && forms.any { it.owner == codeExchange.owner } &&
            forms.all { form -> form.method.classDef.superclassChain.any { it.descriptor == authHeader.owner } }
        ) {
            "Expected the rif authorization code, guest and refresh forms."
        }
        userAgent.method
        requestUserAgent.method

        authorizeClient.before { capture("clientId").assign(string(config.clientId)) }
        for (point in listOf(authorizeRedirect, exchangeRedirect, callbackRedirect)) {
            point.after { capture("redirect").assign(string(config.redirect)) }
        }
        callbackPredicate.redirectTo(OAuthRedirect.matches)
        webCallback.before {
            // The original success path also checks state. Apply the same check to OAuth errors.
            whenTrue(OAuthRedirect.matches(param(1), string(config.redirect))) {
                val uri = callStatic(URI, "parse", "(Ljava/lang/String;)Landroid/net/Uri;", param(1))
                val state = uri.callVirtual(URI, "getQueryParameter", "(Ljava/lang/String;)Ljava/lang/String;",
                    string("state"))
                whenFalse(call(loginSession).call(stateCheck, state)) { returnTrue() }
            }
        }
        authHeader.replace {
            param(1).call(authHeaderSetter, string("Authorization"), string(config.basicAuthorization))
            returnVoid()
        }
        userAgent.alwaysReturn(config.userAgent)
    }
}
