// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.patches.rifisfun.misc.sharelinks

import app.edbfi.extension.rifisfun.reading.bindings.ShareLinks
import app.edbfi.patches.rifisfun.RifPatchStrings
import app.edbfi.patches.rifisfun.misc.medialinks.openLink
import app.edbfi.patches.rifisfun.rifIsFun
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch

private val dispatchIncoming = method("dispatch incoming Reddit link") {
    strings("reddit url type %s", "android.intent.action.VIEW", "thread_uri")
    params()
    returns(Type.Void)
    calls { owner("android.content.Intent"); name("getData") }
}

val fixShareLinks = patch(RifPatchStrings.fixShareLinks) {
    description(RifPatchStrings.fixShareLinksDescription)
    compatibleWith(rifIsFun)
    execute {
        val types = openLink.parameterTypes
        check(dispatchIncoming.owner == "Lcom/andrewshu/android/reddit/MainActivity;")
        ShareLinks.resumeInApp.implement {
            call(openLink, param(0), param(1), param(2).cast(types[2]), param(3), param(4), param(5),
                param(6).cast(types[6]), param(7).cast(types[7]), param(8), param(9).cast(types[9]))
            returnVoid()
        }
        openLink.before {
            whenTrue(ShareLinks.resolveInApp(param(0), param(1), param(2), param(3), param(4),
                param(5), param(6), param(7), param(8), param(9))) { returnVoid() }
        }
        dispatchIncoming.before {
            whenTrue(ShareLinks.resolveIncoming(thisObject)) { returnVoid() }
        }
    }
}
