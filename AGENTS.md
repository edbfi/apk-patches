# Agent instructions

Patches by edbfi for several Android apps, in two bundles from two independent Gradle builds: `revanced/` builds
the ReVanced bundle, `patches-<version>.rvp`, and `morphe/` the Morphe bundle, `patches-<version>.mpp`. Nothing is
shared between them at build time, and a change to one never needs a change to the other. The rules below name
`revanced/`'s paths relative to it; `morphe/` has the same shape and follows the same rules with Morphe's API
(`patch` with `execute {}`, `Fingerprint` objects in `Fingerprints.kt`, each matched with `matchSingle()`), `.mpe`
extension files and its pages in `docs/morphe/`.
CI comes from [edbfi-ci](https://github.com/edbfi/edbfi-ci); its rules apply here. `CLAUDE.md` is a symlink to this
file.

## Layout

- A patch lives in `patches/src/main/kotlin/app/edbfi/patches/<app>/<area>/<feature>/`: `<Feature>Patch.kt`
  and its method matchers in `Matchers.kt`. `<app>` is a short lowercase app name (`syncforreddit`), `<area>`
  groups features (`ads`, `api`, `layout`, `misc`). Resources go in
  `patches/src/main/resources/edbfi/<app>/<feature>/`; code several apps share goes in `patches/…/patches/shared/`.
- Java that runs inside an app goes in an extension module, `extensions/<app>/`, package
  `app.edbfi.extension.<app>`, with compile-only stubs of the app's classes in its `stub/` subproject. Add it with
  the first patch that needs it, name its file `extensions/edbfi/<app>.rve` with the plugin's `extension { name }`,
  and keep kotlin-stdlib out of its runtime classpath. Extension modules share `extensions/proguard-rules.pro` and
  `build.gradle.kts`'s `android.library apply false`; building them needs an Android SDK (`ANDROID_HOME`).
- Every bundle a user loads shares one class loader, so a bundle file another bundle also has can be read in place
  of ours. Hence the `edbfi` in resource and extension paths; `PatchMetadataTest` checks it.

## Local files

- Never commit APKs, bundles, keystores or app code. Local files go in the main checkout's ignored `tmp/`:
  `inputs/<name>-<version>/` for downloads, one version per directory; `decompiled/<app>-<version>/`;
  `repos/` for other projects' clones; `tools/<purpose>/` for our scripts; `evidence/<feature>/`;
  `releases/<version>/` for release assets and notes; and `archive/` for finished work. `<app>` is as in its
  package dir; `<name>` is `<app>` for our apps and the project's repo name otherwise (`revanced-manager`).
  `just revanced-patch` writes the current checkout's `tmp/out/`, and the device-test skill writes
  `tmp/device-test/<run>/`.
- Fetch an APK yourself with `agent-browser`, which runs cloakbrowser for Cloudflare's check, from APKMirror,
  APKPure or a similar site.

## Patches

- Patcher v22 API only: `bytecodePatch`/`resourcePatch` with `apply {}` and declarative method matchers, never
  `execute {}` or `fingerprint {}`.
- The name says what the patch does ("Remove ads"); the description says it in third person ("Removes ads.").
  Every named patch has a description and a `compatibleWith` naming the package and the versions it was tested
  on. `PatchMetadataTest` checks these and that names are unique per app.
- Match on stable facts: strings, types, access flags, opcodes. Use an obfuscated name only when nothing stable
  exists, and say so in a comment.
- More than a few injected instructions belong in extension Java; the smali only calls into it.
- Fail at patch time: throw `PatchException` when a match is missing or ambiguous rather than risk a
  `VerifyError` in the app. Look up and check everything a patch needs before its first change: the patcher keeps
  a failed patch's changes, and the CLI still writes the APK. A shared hook patch must leave the app no worse until
  a feature turns it on.
- Helpers are `internal`; only patches are public.

## Commands

- `just check` runs what CI runs, prek hooks included. `just build` builds both bundles; `just revanced-check`,
  `just morphe-check` and their `-build` recipes do one. `just revanced-patch <apk>` applies the ReVanced bundle
  with the pinned revanced-cli into `tmp/out/`, and `just morphe-patch <apk>` the Morphe bundle with the pinned
  morphe-desktop CLI. Both sign with the main checkout's `tmp/test-signing.keystore`, so every build of either
  ecosystem installs over the last with `adb install -r` and keeps the app's data.
- Gradle needs `githubPackagesUsername` and `githubPackagesPassword` (a classic PAT with only `read:packages`)
  in `~/.gradle/gradle.properties`. Its daemon runs on JDK 25 (`gradle/gradle-daemon-jvm.properties`); point
  `org.gradle.java.installations.paths` there at a JDK 25 that Gradle doesn't find by itself, such as Homebrew's.

## Test device

- `just device` starts Android 15 in Docker ([Redroid](https://github.com/remote-android/redroid-doc), no VM or
  KVM) and connects adb on loopback; use `ANDROID_SERIAL=127.0.0.1:5600`. Agents share it: take turns.
  `just device-reset` deletes it and its data, the owner's login included: run it only when the owner asks.
- The host kernel needs binderfs and PSI; where PSI is off by default, boot with `psi=1`. On arm64 Linux, use a
  native `adb` on `PATH`, not the SDK's x86_64 one.
- It renders in software, and playing a video crashes its SurfaceFlinger
  ([redroid-doc#920](https://github.com/remote-android/redroid-doc/issues/920)): turn autoplay off and check
  videos through downloaded files and logs.
- Reddit's login page rejects Redroid's WebView (Chromium 128), so on arm64 `just device` installs a pinned
  Chromium WebView over it, re-signed with AOSP's public test key; the first run needs `ANDROID_HOME` for
  `apksigner`.
- Agents never type account credentials. The owner logs in through scrcpy; the agent then keeps the login by
  updating with `adb install -r`, never uninstalling.

## rea

- `.mcp.json`, `.codex/config.toml` and `opencode.json` start `scripts/rea.sh`, which runs rea from the main
  checkout's ignored `.rea/install/`, shared by every worktree. `just setup-rea` installs the current checkout's
  pin (`.rea/package.json`) there, fetches its JADX bridge, re-syncs `.agents/skills/reverse-engineer-anything`
  from the installed package, and checks Ghidra and JADX.
- Machine tools: Homebrew `openjdk@25` and `apktool`, and `adb`. rea verifies its Ghidra bridge against 12.1.4
  only and runs later 12.x, such as Homebrew's 12.2, unverified, so Ghidra 12.1.4 comes from NSA's release zip into
  `~/.local/opt/ghidra_12.1.4_PUBLIC` (or set `GHIDRA_INSTALL_DIR`); on arm64, build its decompiler with
  `support/gradle/gradlew buildNatives`.
- On arm64 Linux, x86_64 Android tools such as aapt2 run through `qemu-user-binfmt` with `libc6-amd64-cross`.
- Bump rea by hand: change `.rea/package.json`, refresh the lock with
  `npm install --package-lock-only --prefix .rea`, run `just setup-rea`, and commit the skill changes with it.

## Docs

- Docs hold only what the code can't say: this file, `README.md` and `docs/` for users, and short comments next to
  the code they explain. No reports, plans or notes in the repo: evidence goes in the PR body.
- When a fact changes, fix or delete every other statement of it; never append history.
- `README.md` lists each app in Supported apps (name, package, and a link to its page named by the number of
  patches) and holds what all apps share, such as Install. Each app has a page, `docs/revanced/<app>.md` (`<app>`
  as in its package dir), titled with its name: Tested version, Patches (name and description as in the code,
  On/Off default), Options (what they are for, not their defaults) and Known issues (bold symptom, then cause;
  deleted once fixed). Leave out an empty Options or Known issues. `ReadmeTest` checks the app table and each page
  against the bundle.

## Commits and releases

- Conventional Commits scoped by app (`feat(syncforreddit): …`) or by area (`ci`, `rea`), with a DCO sign-off
  (`git commit -s`), through a PR.
- New source files carry `SPDX-License-Identifier: AGPL-3.0-or-later`. Other patch repos are reference only:
  nothing is copied from them.
- The bundle has one [SemVer](https://semver.org/) version. Release by publishing a GitHub release from main
  tagged `vX.Y.Z`; `release.yml` builds, signs and attaches the assets. Never move, delete or reuse a tag; fix a
  bad release with a new one.
- Bump from the commits since the last tag: breaking → major (minor while the version is `0.y.z`), `feat` → minor,
  `fix` or `perf` → patch. A change is breaking when users must act on it: a removed or renamed patch or option, or
  a dropped app version. Mark it with `!`.
