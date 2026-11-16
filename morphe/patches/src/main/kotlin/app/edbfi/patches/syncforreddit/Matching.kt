// SPDX-License-Identifier: AGPL-3.0-or-later
package app.edbfi.patches.syncforreddit

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// R8 renamed Sync's classes but kept each one's SourceFile attribute, so its fingerprints name the class by that.

/**
 * Whether the method has every one of [flags], and maybe others. A fingerprint's `accessFlags` must equal the
 * method's; these fingerprints keep the looser check the ReVanced patches make.
 */
internal fun Method.hasFlags(vararg flags: AccessFlags) = flags.all { it.isSet(accessFlags) }

/** A fingerprint condition: the method's class is named [sourceFile] in its SourceFile attribute, and [and] holds. */
internal fun declaredIn(
    sourceFile: String,
    and: (Method) -> Boolean = { true },
): (Method, ClassDef) -> Boolean = { method, classDef -> classDef.sourceFile == sourceFile && and(method) }

/** Whether [reference] names [method]: same class, name, parameter types and return type. */
internal fun isCallTo(
    reference: MethodReference?,
    method: MethodReference,
) = reference != null && reference.definingClass == method.definingClass && reference.name == method.name &&
    reference.parameterTypes.map { it.toString() } == method.parameterTypes.map { it.toString() } && reference.returnType == method.returnType

/** The method of the class named [sourceFile] that holds every one of [strings], each a complete literal. */
internal open class SourceStringsFingerprint(
    sourceFile: String,
    vararg strings: String,
) : Fingerprint(
    strings = strings.toList(),
    custom = declaredIn(sourceFile),
)
