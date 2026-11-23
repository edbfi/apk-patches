// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.patches.rifisfun.misc.goldenplatinum

import app.edbfi.patches.rifisfun.RifPatchStrings
import app.edbfi.patches.rifisfun.ads.disable.screenTransition
import app.reseam.patch.CodeScope
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

private const val RIF = "com.andrewshu.android.reddit."
private const val PREFERENCE_SCREEN = "general_preferences"
private const val SHOW_ADS = "edbfi_show_ads"
// rif 5.6.22 ships RecyclerView.Adapter under this R8 name in the album helper signature.
private const val RECYCLER_ADAPTER = "androidx.recyclerview.widget.RecyclerView\$h"

private val applicationContext = method("read rif application context") {
    inClass(klass(RIF + "RedditIsFunApplication"))
    flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
    params()
    returns("android.content.Context")
}

private val insertFeedAd = method("allow an ad row in a thread feed") {
    // Disable ads erases the identifying theme calls. Two protected ()Z overrides remain,
    // so pin the R8 class and override names from rif 5.6.22.
    inClass(klass("e5.e0"))
    name("d0")
    flags(AccessFlags.PROTECTED)
    params()
    returns(Type.Boolean)
}

private val showBanner = method("allow a banner below the thread list") {
    // Three private methods on this enum accept MainActivity and return Boolean. Disable ads
    // erases the theme call that distinguishes the gate, so retain its rif 5.6.22 R8 name.
    inClass(screenTransition)
    name("b")
    flags(AccessFlags.PRIVATE)
    params(RIF + "MainActivity")
    returns(Type.Boolean)
}

private val loadFeedAds = method("load ads for the thread feed") {
    inClass(klass(RIF + "ads.AdViewHelper"))
    name("checkConditionsAndLoadInitialAds")
    params()
    returns(Type.Void)
}

private val wrapAlbumAdapter = method("add ad rows to an image album") {
    inClass(klass(RIF + "ads.ImageAlbumAdViewHelper"))
    name("newRecyclerAdapter")
    params("android.app.Activity", RECYCLER_ADAPTER)
    returns(RECYCLER_ADAPTER)
}

// RifBaseSettingsFragment.F4() selects the "settings" preference file in rif 5.6.22.
private fun CodeScope.showAds() =
    call(applicationContext)
        .callVirtual("android.content.Context", "getSharedPreferences",
            "(Ljava/lang/String;I)Landroid/content/SharedPreferences;", string("settings"), int(0))
        .callInterface("android.content.SharedPreferences", "getBoolean",
            "(Ljava/lang/String;Z)Z", string(SHOW_ADS), bool(true))

internal fun PatchRuntime.prepareAdDisplaySetting(): () -> Unit {
    applicationContext.method
    insertFeedAd.method
    showBanner.method
    loadFeedAds.method
    wrapAlbumAdapter.method
    stockPreferenceScreen(PREFERENCE_SCREEN).use { screen ->
        check(screen.root.tag == "PreferenceScreen") {
            "Expected PreferenceScreen at the root of $PREFERENCE_SCREEN, found ${screen.root.tag}"
        }
        screen.requirePreference("ADS_ENABLED", "CheckBoxPreference")
        check(screen.findByAttribute("android:key", SHOW_ADS).isEmpty()) {
            "Unexpected existing preference with android:key=$SHOW_ADS in $PREFERENCE_SCREEN"
        }
    }
    val strings = checkNotNull(RifPatchStrings::class.java.getResourceAsStream("/edbfi/rifisfun/strings.xml"))
        .use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).getElementsByTagName("string") }
    val labels = (0 until strings.length).map { strings.item(it) as Element }
        .associate { it.getAttribute("name") to it.textContent }
    val title = labels.getValue(RifPatchStrings.showAds.name)
    val summary = labels.getValue(RifPatchStrings.showAdsSummary.name)

    return {
        val titleId = checkNotNull(resources.addString(SHOW_ADS, title))
        val summaryId = checkNotNull(resources.addString("${SHOW_ADS}_summary", summary))
        stockPreferenceScreen(PREFERENCE_SCREEN).use { screen ->
            val checkbox = screen.createElement("CheckBoxPreference")
            checkbox["android:key"] = SHOW_ADS
            checkbox.setResourceRef("android:title", titleId)
            checkbox.setResourceRef("android:summary", summaryId)
            checkbox.setBool("android:defaultValue", true)
            screen.root.appendChild(checkbox)
        }
        insertFeedAd.before { whenFalse(showAds()) { returnValue(bool(false)) } }
        showBanner.before { whenFalse(showAds()) { returnValue(bool(false)) } }
        loadFeedAds.before { whenFalse(showAds()) { returnVoid() } }
        // The stock helper accepts its original adapter in its position and lifecycle methods.
        wrapAlbumAdapter.before { whenFalse(showAds()) { returnValue(param(1)) } }
    }
}
