# Sync for Reddit

## Compatible version

| Package | Compatible version |
| --- | --- |
| `com.laurencedawson.reddit_sync` | `v23.06.30-13:39`, `23033` |

## Patches

| Patch | What it does | Default |
| --- | --- | --- |
| Disable ads | Removes ads and the ad consent form, and keeps the ad SDKs from starting. | On |
| Disable Sync for Lemmy bottom sheet | Prevents the Sync for Lemmy bottom sheet from opening at startup. | On |
| Fix /s/ links | Opens Reddit share links (/s/) as the post they point to, from the app and from other apps. | On |
| Fix galleries | Restores loading Reddit galleries from the post listing. | On |
| Fix Imgur | Loads Imgur albums and covers and uploads images through the Imgur API. | Off |
| Fix Other discussions | Fixes “Other discussions” failing to load, by searching Reddit for the link instead. | On |
| Fix post thumbnails | Fixes post thumbnails not loading. | On |
| Fix Tenor previews | Loads missing Tenor thumbnails from public GIF pages. | On |
| Fix video downloads | Fixes downloaded Reddit videos having only audio. | On |
| Fix View more comments | Fixes “View more” not loading further comments. | On |
| Fix website previews and hide retired cloud backup | Loads eligible website previews directly in comments and link options. Hides retired cloud backup controls and the Pushshift comment restore action. Keeps local backup and restore available. Stores user tags and lets guests color comment authors on the device. Lets guests extract selectable text from images on the device. Lets guests translate comments and text posts on the device. | On |
| Hide Gfycat link previews | Hides broken Gfycat link previews. | On |
| Use /user/ endpoint | Requests user profiles from /user/ instead of /u/. | Off |

**Settings → Link handling → Website previews** controls Open Graph images in comments and in **Link options**
when you long-press a link. It is on by default, uses Sync’s existing per-account setting and appears in Settings
Search. Signed-out profiles can use it. Sync’s usual website URL exclusions apply. Loading contacts the linked
site and its image host. Tap a failed image in **Link options** to retry. YouTube links use Sync’s separate preview
handler, which still requires Ultra access.

Sync’s comment restore action used a retired Pushshift integration. The patch also hides its
**Restore removed comments** entry in Ultra settings and Settings Search.

Open **Comment options → Local author color** to pick a color for the author. Use **Settings → Sync Ultra →
Local author colors** to edit or remove saved colors. Both controls work while signed out. Colors are stored
with the active profile and highlight that author’s name in comments.

Open **Image options → Extract text** to use Sync’s on-device recognizer as a guest. Its result sheet provides
selection and **Copy text**. Device testing covered unavailable Play services and image load failures.
Successful recognition and copying still need verification on a device with the Latin recognition module.

Use **Translate comment with Google** in comment options or **Translate text with Google** in a text post’s
options. **Translate all** in discussion actions translates eligible items in the loaded discussion. Use
**Action visibility → Translate** to show or hide that bulk action. Translations appear in English below the
original text. If Sync cannot identify the language of a single item, choose its source language or go back.

## Options

### Fix Imgur

`Imgur client ID` identifies an Imgur application you are authorized to use. Reseam stores the ID in the
patched APK so the app can request albums and covers. This option has no preset ID.

## Known issues

- **Other discussions can miss posts or show posts with different links.** It uses Reddit search.
- **Some Tenor thumbnails stay blank.** Sync contacts the public GIF page, which must publish a preview image.
- **Extract text can report that Google Play services is unavailable.** Sync relies on Play services to supply
  the ML Kit Latin recognition module. If the module is downloading, wait for it to finish and retry.
- **Automatic translation language detection needs Google Play services and its ML Kit language identification
  module.** First use for each source language can require a translation model download over Wi-Fi. Automatic
  language detection and a successful **Translate all** run still need device verification.
- **Website previews can show a missing image.** Sync checks up to two distinct public `og:image` URLs in page order within one request budget. It skips previews when a proxy is configured and discards a loaded image if previews are off or a proxy is configured when loading finishes.
- **Tags and author colors do not sync or clear in bulk.** Sync used its retired cloud service for both actions.
  In the built-in managers, **Force refresh** does nothing and **Clear all** shows “TODO” where those actions appear.
  Remove entries individually. The tag manager hides **Import local**, which uploaded tags.
