// SPDX-License-Identifier: AGPL-3.0-or-later
package app.edbfi.patches.syncforreddit.misc.sharelinks

import app.edbfi.extension.syncforreddit.sharelinks.bindings.ShareLinks
import app.edbfi.patches.syncforreddit.SyncPatchStrings
import app.edbfi.patches.syncforreddit.syncForReddit
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch

private const val CONTEXT = "Landroid/content/Context;"
private const val STRING = "Ljava/lang/String;"

private val routeLink = method("Sync link router") {
    strings("Opening link: ", "Was shortened")
    returns("Z")
    param(0, CONTEXT)
    param(3, STRING)
    paramCount(10)
}

private val openLink = method("Sync link router entry point") {
    params(CONTEXT, STRING)
    returns("Z")
    calls(routeLink)
}

val fixShareLinks = patch(SyncPatchStrings.fixShareLinks) {
    description(SyncPatchStrings.fixShareLinksDescription)
    compatibleWith(syncForReddit)

    execute {
        routeLink.method
        openLink.method
        ShareLinks.openResolved.implement {
            call(openLink, param(0), param(1))
            returnVoid()
        }
        routeLink.before {
            whenTrue(ShareLinks.resolve(param(0), param(3))) {
                returnTrue()
            }
        }
    }
}
