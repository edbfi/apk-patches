<p align="center">
  <img src="assets/apk-patches.svg" alt="apk-patches logo" width="160" height="160">
</p>
<h1 align="center">apk-patches</h1>
<p align="center"><strong>Signed patches for Android apps, for ReVanced and Morphe.</strong></p>
<p align="center">
  <a href="https://github.com/edbfi/apk-patches/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/license-AGPL--3.0--or--later-blue?style=flat" alt="License: AGPL-3.0-or-later">
  </a>
  <a href="https://github.com/JetBrains/kotlin" title="Kotlin source">
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat&amp;logo=kotlin&amp;logoColor=white" alt="Kotlin">
  </a>
  <a href="https://github.com/ReVanced/revanced-patcher" title="ReVanced Patcher framework source">
    <img src="https://img.shields.io/badge/ReVanced-0076FF?style=flat" alt="ReVanced">
  </a>
  <a href="https://github.com/MorpheApp/morphe-patcher" title="Morphe Patcher framework source">
    <img src="https://img.shields.io/badge/Morphe-1C1B1F?style=flat" alt="Morphe">
  </a>
  <a href="https://android.googlesource.com/platform/manifest" title="Android Open Source Project source manifest">
    <img src="https://img.shields.io/badge/Android-3DDC84?style=flat&amp;logo=android&amp;logoColor=black" alt="Android">
  </a>
  <a href="https://github.com/edbfi/apk-patches/actions/workflows/ci.yml?query=branch%3Amain"><img src="https://img.shields.io/github/actions/workflow/status/edbfi/apk-patches/ci.yml?branch=main&amp;event=push&amp;label=CI&amp;style=flat" alt="CI"></a>
</p>

Patches for Android apps, in two signed bundles: `patches-X.Y.Z.rvp` for [ReVanced](https://revanced.app) and
`patches-X.Y.Z.mpp` for [Morphe](https://morphe.software). Each release carries both.

## Supported apps

| App | Package | ReVanced | Morphe |
| --- | --- | --- | --- |
| rif is fun | `com.andrewshu.android.reddit` | [12 patches](docs/revanced/rifisfun.md) | [12 patches](docs/morphe/rifisfun.md) |
| Sync for Reddit | `com.laurencedawson.reddit_sync` | [15 patches](docs/revanced/syncforreddit.md) | [15 patches](docs/morphe/syncforreddit.md) |

Each link opens that bundle's page for the app: its patches, their options and known issues, and the app version they
were tested on. The two bundles are built from separate code, so their patches can differ. Patch the tested version:
ReVanced Manager and both CLIs skip these patches on other versions unless you force them, Morphe Manager asks you to
confirm, and other versions are untested.

## Install

Both managers read a feed named `patches-bundle.json`, but not the same one. The one at the root of this repository is
Morphe's; ReVanced Manager can't read it. Use the URL each section below gives.

### ReVanced Manager

Add a patches source with this URL:

```text
https://github.com/edbfi/apk-patches/releases/latest/download/patches-bundle.json
```

Manager also selects the patches its built-in ReVanced Patches has for the same app, and some of them change the same
code as ours. Before you patch, turn on Settings → Advanced → **Allow changing patch selection and options**, then in
**Select patches** untick **ReVanced Patches**.

Manager doesn't verify signatures. To check a bundle, use the ReVanced CLI.

### ReVanced CLI

Download `patches-X.Y.Z.rvp`, `.rvp.asc` and `.rvp.sigstore.json` from a
[release](https://github.com/edbfi/apk-patches/releases) and the [public key](keys/release-signing.asc), then:

```sh
java -jar revanced-cli.jar patch \
  -p patches-X.Y.Z.rvp -k release-signing.asc -s patches-X.Y.Z.rvp.asc \
  -a patches-X.Y.Z.rvp.sigstore.json -r edbfi/apk-patches \
  app.apk
```

The key's fingerprint is `7B40 A51F C15C 376F 09D1  6979 893C 92F2 3E64 7FC8`.

### Morphe Manager

Open **Sources**, tap **+**, paste this URL and tap **Add**:

```text
https://github.com/edbfi/apk-patches
```

Manager downloads the latest `patches-X.Y.Z.mpp`, checks the signature inside it and remembers the key. It won't install
a later release signed by another key without asking you. The source's **Signed by** reads `bb0f 2749 5a09 70ea`.

Manager's built-in Morphe Patches has no patches for these apps, so there is nothing to untick.

Manager signs patched apps with its own key, and Android won't install them over an app signed with another, such as
one ReVanced Manager patched: you would have to uninstall that first, which erases its data. To keep the data, import
the key it was signed with in Settings → **Signing key** before you patch.

### Morphe CLI

Download `morphe-desktop-<version>-all.jar` from
[Morphe desktop's releases](https://github.com/MorpheApp/morphe-desktop/releases), then:

```sh
java -jar morphe-desktop-<version>-all.jar patch -p https://github.com/edbfi/apk-patches app.apk
```

Like Manager, the CLI downloads the latest bundle, checks its signature and remembers the key. It also takes a
downloaded file, `-p patches-X.Y.Z.mpp`, but doesn't check a file's signature. Check it yourself first: download
`patches-X.Y.Z.mpp` and `.mpp.asc` from a [release](https://github.com/edbfi/apk-patches/releases) and the
[public key](keys/release-signing.asc) (fingerprint above), then:

```sh
gpg --import release-signing.asc
gpg --verify patches-X.Y.Z.mpp.asc patches-X.Y.Z.mpp
gh attestation verify patches-X.Y.Z.mpp --repo edbfi/apk-patches
```

The CLI signs patched APKs with signature scheme v2 only, so they install on Android 7.0 or later.

## Report a problem

[Open an issue](https://github.com/edbfi/apk-patches/issues/new/choose) with the form that fits: **Patching
fails** if patching stops or the APK won't install, **Bug in a patched app** if the app misbehaves after patching.
Both forms ask which bundle you patched with, ReVanced or Morphe, and require a log, so have it ready; the form says
where to find it. Problems with ReVanced Manager, the ReVanced CLI or ReVanced's own patches go to
[ReVanced](https://github.com/ReVanced), and those with Morphe Manager, the Morphe CLI or Morphe's own patches to
[Morphe](https://github.com/MorpheApp).

## Request a patch

Use the [Patch request](https://github.com/edbfi/apk-patches/issues/new?template=3-patch-request.yml) form, for a
new app or one already supported, and say which bundle you want it in: ReVanced, Morphe or both.

## Contribute

Build commands, patch rules and commit conventions are in [AGENTS.md](AGENTS.md).

## License

Original code and artwork by edbfi are licensed under the [GNU Affero General Public License](LICENSE),
version 3 or (at your option) any later version (`AGPL-3.0-or-later`).

The vendored [REA skill](.agents/skills/reverse-engineer-anything.LICENSE) remains MIT-licensed. The Gradle
wrapper remains Apache-2.0-licensed, and dependencies retain their respective licenses.
