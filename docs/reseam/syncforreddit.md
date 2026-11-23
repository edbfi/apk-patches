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
| Fix website previews and hide retired cloud backup | Loads eligible website previews directly in comments and link options. Hides retired cloud backup controls and the Pushshift comment restore action. Keeps local backup and restore available. Stores user tags and paints on the device. | On |
| Hide Gfycat link previews | Hides broken Gfycat link previews. | On |
| Use /user/ endpoint | Requests user profiles from /user/ instead of /u/. | Off |

**Settings → Link handling → Website previews** controls Open Graph images in comments and in **Link options**
when you long-press a link. It is on by default, uses Sync’s existing per-account setting and appears in Settings
Search. Signed-out profiles can use it. Sync’s usual website URL exclusions apply. Loading contacts the linked
site and its image host. Tap a failed image in **Link options** to retry.

Sync’s comment restore action used a retired Pushshift integration. The patch also hides its
**Restore removed comments** entry in Ultra settings and Settings Search.

## Options

### Fix Imgur

`Imgur client ID` identifies an Imgur application you are authorized to use. Reseam stores the ID in the
patched APK so the app can request albums and covers. This option has no preset ID.

## Known issues

- **Other discussions can miss posts or show posts with different links.** It uses Reddit search.
- **Some Tenor thumbnails stay blank.** Sync contacts the public GIF page, which must publish a preview image.
- **Website previews can show a missing image.** Sync checks up to two distinct public `og:image` URLs in page order within one request budget. It skips previews when a proxy is configured and discards a loaded image if previews are off or a proxy is configured when loading finishes.
- **Tags and paints do not sync across devices.** Sync used its cloud service to sync and clear entries. This patch
  saves individual changes locally. Where shown, **Force refresh** does nothing, and **Clear all** shows “TODO”.
  Remove entries individually. The tag manager hides **Import local**, which uploaded tags. Paint controls require
  Sync Ultra.
