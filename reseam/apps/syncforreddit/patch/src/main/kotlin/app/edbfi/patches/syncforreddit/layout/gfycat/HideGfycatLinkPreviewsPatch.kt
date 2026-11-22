// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.patches.syncforreddit.layout.gfycat

import app.edbfi.patches.syncforreddit.SyncPatchStrings
import app.edbfi.patches.syncforreddit.syncForReddit
import app.reseam.patch.CodeScope
import app.reseam.patch.FieldTarget
import app.reseam.patch.Type
import app.reseam.patch.ValueRef
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.fieldRef
import app.reseam.patch.dex.invokeRegisters
import app.reseam.patch.dex.methodRef
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.regA
import app.reseam.patch.dex.regB
import app.reseam.patch.dex.ref
import app.reseam.patch.dex.stringValue
import app.reseam.patch.dex.typeRef
import app.reseam.patch.fieldTarget
import app.reseam.patch.method
import app.reseam.patch.methodTarget
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.types.Instruction
import app.reseam.patch.types.MethodRef

// Sync 23033's RedditLinkPreview constructor (wa.e) assigns 3 to its inherited media type.
private const val PLAIN_LINK = 3
private const val STRING_BUILDER = "Ljava/lang/StringBuilder;"

private val linkHost = method("Link host parser") {
    params(Type.String)
    returns(Type.String)
    flags(AccessFlags.STATIC)
    strings("www.")
    calls(MethodRef("Ljava/net/URI;", "create", "(Ljava/lang/String;)Ljava/net/URI;"))
    calls(MethodRef("Ljava/net/URI;", "getHost", "()Ljava/lang/String;"))
}

private val isGfycatLink = method("Gfycat link classifier") {
    params(Type.String)
    returns(Type.Boolean)
    strings("gfycat.com", "http://gfycat.com/", "https://gfycat.com/")
    calls(linkHost)
}

private fun CodeScope.whenGfycat(url: ValueRef, block: CodeScope.() -> Unit) {
    whenNotNull(url) {
        // The stock parser returns a lowercase URI host, or null for invalid URLs.
        val host = call(linkHost, url)
        whenNotNull(host) {
            whenTrue(host.callVirtual(
                Type.String, "matches", "(Ljava/lang/String;)Z", string("""(?:.+\.)?gfycat\.com\.?"""),
            ), block)
        }
    }
}

private val postMedia = method("Post media parser") {
    strings("https://thumbs.gfycat.com/", "-poster.jpg", "USING YOUTUBE")
    calls(isGfycatLink)
}

private val commentLink = method("Comment link renderer") {
    strings("tenor.com", ".jpeg", "Is user link: ")
    calls(isGfycatLink)
}

private val postDescription = method("Post model field labels") {
    name("toString")
    params()
    returns(Type.String)
    custom { !isStatic }
    strings("Post{id=", ", sync_type=", ", raw_url=", ", thumbnail=", ", preview_url=")
}

private fun Instruction.isAppend(type: String) =
    (opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
        methodRef == MethodRef(STRING_BUILDER, "append", "($type)$STRING_BUILDER")

private fun postField(label: String, read: Opcode, type: String) = fieldTarget("Post $label field") {
    val model = postDescription.method
    val instructions = model.instructions
    val labels = instructions.indices.filter { instructions[it].stringValue == ", $label=" }
    check(labels.size == 1) { "Post $label field: expected one label, found ${labels.size}" }
    val index = labels.single()
    val expected = "Post $label field: expected contiguous label append, $read from this, and value append"
    check(index + 3 < instructions.size) { expected }
    val (labelLoad, labelAppend, fieldRead, valueAppend) = instructions.subList(index, index + 4)
    val labelRegister = labelLoad.regA
    val builderRegister = labelAppend.invokeRegisters?.firstOrNull()
    val field = fieldRead.fieldRef
    val valueRegister = fieldRead.regA
    val thisRegister = model.registersSize - model.insSize
    check(
        labelRegister != null && builderRegister != null && valueRegister != null &&
            (labelLoad.opcode == Opcode.CONST_STRING || labelLoad.opcode == Opcode.CONST_STRING_JUMBO) &&
            builderRegister != labelRegister && builderRegister != valueRegister && builderRegister != thisRegister &&
            labelAppend.isAppend(Type.String) && labelAppend.invokeRegisters == listOf(builderRegister, labelRegister) &&
            fieldRead.opcode == read && fieldRead.regB == thisRegister &&
            field != null && field.definingClass == model.owner && field.fieldType == type &&
            model.classDef.instanceFields.any { it.ref == field } &&
            valueAppend.isAppend(type) && valueAppend.invokeRegisters == listOf(builderRegister, valueRegister),
    ) { expected }
    field
}

private val rawUrl = postField("raw_url", Opcode.IGET_OBJECT, Type.String)
private val syncType = postField("sync_type", Opcode.IGET, Type.Int)
private val thumbnail = postField("thumbnail", Opcode.IGET_OBJECT, Type.String)
private val previewUrl = postField("preview_url", Opcode.IGET_OBJECT, Type.String)

private fun postGetter(label: String, read: Opcode, returnOpcode: Opcode, field: FieldTarget) =
    methodTarget("Post $label getter") {
        val ref = field.ref
        val matches = postDescription.method.classDef.methods.filter { method ->
            !method.isStatic && method.parameterTypes.isEmpty() && method.returnType == ref.fieldType &&
                method.instructions.let { instructions ->
                    instructions.size == 2 && instructions[0].opcode == read && instructions[0].fieldRef == ref &&
                        instructions[0].regB == method.registersSize - method.insSize &&
                        instructions[1].opcode == returnOpcode && instructions[1].regA == instructions[0].regA
                }
        }
        check(matches.size == 1) { "Post $label getter: expected one method, found ${matches.size}" }
        matches.single()
    }

private val postType = postGetter("sync_type", Opcode.IGET, Opcode.RETURN, syncType)
private val postThumbnail = postGetter("thumbnail", Opcode.IGET_OBJECT, Opcode.RETURN_OBJECT, thumbnail)
private val postPreview = postGetter("preview_url", Opcode.IGET_OBJECT, Opcode.RETURN_OBJECT, previewUrl)

val hideGfycatLinkPreviews = patch(SyncPatchStrings.hideGfycatLinkPreviews) {
    description(SyncPatchStrings.hideGfycatLinkPreviewsDescription)
    compatibleWith(syncForReddit)

    execute {
        val postCheck = postMedia.point { calls(isGfycatLink) }.captureArgumentAs("url", 0)
        // The app has no stable names for its media model. Resolve the plain-link model and
        // URL field from the parser's existing plain-link path.
        val plainLink = postMedia.point { string("mediadownloads.mlb.com") }
            .next { opcode(Opcode.NEW_INSTANCE) }
        val plainLinkType = checkNotNull(plainLink.instruction.typeRef)
        val mediaUrl = plainLink.next { opcode(Opcode.IPUT_OBJECT) }.field()

        val commentPreviewBranch = commentLink.point {
            invokeVirtual { owner(Type.String); name("contains") }
            argument(1) { string("tenor.com") }
        }.captureArgumentAs("url", 0)
            .next {
                resultOf(Type.Boolean)
                then { opcode(Opcode.IF_EQZ) }
                then { opcode(Opcode.MOVE) }
                then { opcode(Opcode.IF_EQZ) }
            }
            .captureAs("showPreview", Type.Boolean)

        // Resolve all targets before editing.
        postCheck.index
        commentPreviewBranch.index
        val linkClass = checkNotNull(bytecode.findClass(plainLinkType)) { "Missing plain-link model" }
        check(linkClass.method("<init>", "()V") != null) { "Missing plain-link constructor" }
        check(
            mediaUrl.type == Type.String && (listOf(linkClass) + linkClass.superclassChain).any { owner ->
                owner.instanceFields.any { it.ref == mediaUrl.ref }
            },
        ) { "Unexpected plain-link URL field" }
        check(setOf(rawUrl.ref, syncType.ref, thumbnail.ref, previewUrl.ref).size == 4) {
            "Post model labels must refer to distinct fields"
        }
        postType.method
        postThumbnail.method
        postPreview.method

        postCheck.before {
            val url = capture("url")
            whenGfycat(url) {
                val link = newInstance(plainLinkType)
                link.set(mediaUrl, url)
                returnValue(link)
            }
        }
        commentPreviewBranch.before {
            whenGfycat(capture("url")) {
                capture("showPreview").assign(bool(false))
            }
        }

        // Cached comment threads can supply a post model without going through the media parser.
        postType.before {
            whenGfycat(thisObject.field(rawUrl)) { returnValue(int(PLAIN_LINK)) }
        }
        for (preview in listOf(postThumbnail, postPreview)) {
            preview.before {
                whenGfycat(thisObject.field(rawUrl)) { returnNull() }
            }
        }
    }
}
