# rif is fun

## Compatible version

| Package | Compatible version |
| --- | --- |
| `com.andrewshu.android.reddit` | `5.6.22`, `4622` |

## Patches

| Patch | What it does | Default |
| --- | --- | --- |
| Disable ads | Removes ads and the ad consent form, and keeps the ad SDKs from starting. | On |
| Fix /s/ links | Opens Reddit share links (/s/) as the post they point to, from the app and from other apps. | On |
| Fix galleries | Restores Reddit gallery and inline video lookup and uses available previews for failed gallery images. | On |
| Fix Imgur | Loads Imgur albums and covers through the Imgur API and opens titled album links. | On |
| Fix load more comments | Fixes "load more comments" not loading further comments. | On |
| Fix markdown links | Shows markdown links that Reddit leaves as raw text, such as links to u/name, as links. | On |
| Fix media links | Opens reddit.com/media links as the Reddit-hosted image they wrap. | On |
| Fix YouTube player | Fixes the built-in YouTube player, which showed a black screen and error code 0. | On |
| Hide retired service controls | Hides disabled Synccit and new-post notification settings, new subscriptions, and the remote theme catalog. | On |
| Keep typed text in view | Keeps the line being typed in view when writing a long reply. | On |

## Options

### Fix Imgur

`Imgur client ID` can override rif’s bundled Imgur registration with one you are authorized to use.
Reseam stores the override in the patched APK so rif can request albums and covers.

## Local themes

On standard rif, enable **Ads & pro features** in **Settings → General settings**. In **Theme**, turn on
**Enable developer mode** and tap **Select theme directory**. Open the folder containing the theme’s
`manifest.json`, then tap **Use this folder → Allow**. After editing the files, choose **Reload files**,
then close and reopen rif.

## Known issues

- **Turning off Ads & pro features can also disable cards, viewers and local themes.** Standard rif still uses its original access and purchase rules.
- **Previously enabled Synccit and notification settings can still appear.** The patch preserves rif’s opt-out controls and their availability rules.
- **Some gallery images are skipped.** Reddit supplies neither the full image nor a usable preview for those items.
- **Imgur uploads use the original proxy.** The album and cover patch leaves that upload path in place.
- **Some YouTube videos cannot play in rif.** The video must be available and allow embedding.
