// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.patches.syncforreddit.misc.ultra

import app.edbfi.patches.syncforreddit.SyncPatchStrings
import app.edbfi.patches.syncforreddit.syncForReddit
import app.reseam.patch.Type
import app.reseam.patch.XmlDocument
import app.reseam.patch.alwaysReturn
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points

private const val BACKUP_XML = "cat_backup"
private const val KEY = "android:key"
private const val PREFERENCE = "Landroidx/preference/Preference;"
// Sync's build obfuscates the AndroidX preference click listener interface.
private const val CLICK_LISTENER = "Landroidx/preference/Preference\$d;"
private const val SYNC_PREFERENCE = "com.laurencedawson.reddit_sync.ui.preferences.defaults.SyncPreference"
private const val DIVIDER = "com.laurencedawson.reddit_sync.ui.preferences.custom.DividerPreference"

private val localActions = listOf("backup_dir", "backup_all", "backup_restore")
private val cloudActions = listOf("ultra_backup_all", "ultra_backup_restore", "ultra_backup_delete")

private val configureBackup = methods("Configure backup preferences") {
    strings(*(localActions + cloudActions + "ultra_backup_divider").toTypedArray())
    params("android.os.Bundle", Type.String)
    returns(Type.Void)
    flags(AccessFlags.PUBLIC)
}.single()

private val refreshCloudBackup = methods("Refresh cloud backup availability") {
    strings("backupExistsCallable", "ultra_backup_restore", "ultra_backup_delete")
    params()
    returns(Type.Void)
    flags(AccessFlags.PRIVATE)
    calls {
        owner("com.google.firebase.functions.HttpsCallableReference")
        params("java.lang.Object")
        returns("com.google.android.gms.tasks.Task")
    }
}.single()

private fun XmlDocument.requirePreference(key: String, tag: String) =
    checkNotNull(findByAttribute(KEY, key).singleOrNull()) {
        "Expected one preference with key $key in $BACKUP_XML"
    }.also {
        check(it.tag == tag) { "Expected $key to be a $tag" }
    }

val syncUltra = patch(SyncPatchStrings.fixWebsitePreviewsAndBackup) {
    description(SyncPatchStrings.fixWebsitePreviewsAndBackupDescription)
    compatibleWith(syncForReddit)

    execute {
        val backupXmlId = checkNotNull(resources.id("xml", BACKUP_XML)) { "Missing $BACKUP_XML" }
        check(resources.paths("xml", BACKUP_XML).size == 1) { "Multiple $BACKUP_XML layouts" }
        val inflateBackup = configureBackup.points("Inflate the backup preferences") {
            invokeVirtual {
                params(Type.Int)
                returns(Type.Void)
            }
            argument(1) { literal(backupXmlId.toLong()) }
        }.single()
        val localListeners = localActions.map { key ->
            configureBackup.points("Register the $key listener") {
                string(key)
                then {
                    invokeVirtual {
                        params("java.lang.CharSequence")
                        returns(PREFERENCE)
                    }
                    argument(1) { string(key) }
                }
                then { opcode(Opcode.MOVE_RESULT_OBJECT) }
                then { opcode(Opcode.NEW_INSTANCE) }
                then {
                    invokeDirect {
                        name("<init>")
                        params(configureBackup.owner)
                        returns(Type.Void)
                    }
                }
                then {
                    invokeVirtual {
                        owner(PREFERENCE)
                        params(CLICK_LISTENER)
                        returns(Type.Void)
                    }
                    argument(0) { resultOf(PREFERENCE) }
                }
            }.single().index
        }
        val cloudSetup = configureBackup.points("Start configuring cloud backup") {
            string(cloudActions.first())
        }.single()
        val statusCall = configureBackup.points("Check for a cloud backup") {
            calls(refreshCloudBackup)
        }.single()
        val setupOrder = listOf(inflateBackup.index) + localListeners + listOf(cloudSetup.index, statusCall.index)
        check(refreshCloudBackup.owner == configureBackup.owner && setupOrder.zipWithNext().all { (a, b) -> a < b }) {
            "Unexpected backup preference setup order"
        }

        val applyWebsitePreviews = resolveWebsitePreviewEdits()

        resources.editXml("xml", BACKUP_XML) {
            check(root.tag == "PreferenceScreen") { "Unexpected $BACKUP_XML root" }
            val category = requirePreference("ultra_backup", "PreferenceCategory")
            val divider = requirePreference("ultra_backup_divider", DIVIDER)
            check(listOf(category, divider).all { it.parent?.tag == "PreferenceScreen" } && divider.children.isEmpty()) {
                "Unexpected cloud backup layout"
            }
            val expectedChildren = listOf(
                "com.laurencedawson.reddit_sync.ui.preferences.defaults.CategoryHeaderPreference" to null,
                "com.laurencedawson.reddit_sync.ui.preferences.CategoryInfoHeaderPreference" to null,
            ) + cloudActions.map { SYNC_PREFERENCE to it }
            check(category.children.map { it.tag to it[KEY] } == expectedChildren) {
                "Unexpected preferences in the cloud backup category"
            }
            cloudActions.forEach { requirePreference(it, SYNC_PREFERENCE) }
            localActions.forEach { key ->
                val action = requirePreference(key, SYNC_PREFERENCE)
                val parent = action.parent
                check(
                    parent?.tag == "PreferenceCategory" && parent[KEY] == null &&
                        parent.parent?.tag == "PreferenceScreen"
                ) { "Expected $key in a local backup category" }
            }

            category.remove()
            divider.remove()
        }
        cloudSetup.before { returnVoid() }
        // The backup status event also calls this method after the screen has been created.
        refreshCloudBackup.alwaysReturn()
        applyWebsitePreviews()
    }
}
