# rif is fun

Patches for rif is fun for Reddit. To install them, see the [README](../README.md#install).

## Tested version

| Package | Tested version |
| --- | --- |
| `com.andrewshu.android.reddit` | `5.6.22` |

## Patches

| Patch | What it does | Default |
| --- | --- | --- |
| Disable ads | Removes ads and the ad consent form, and keeps the ad SDKs from starting. | On |
| Enable golden platinum | Enables the golden platinum features that work without rif's servers, and makes the ads setting switch only ads. | On |
| Fix /s/ links | Opens Reddit share links (/s/) as the post they point to, from the app and from other apps. | On |
| Fix galleries | Fixes Reddit galleries and inline Reddit videos failing to load, including gallery images Reddit marks as failed. | On |
| Fix Imgur | Fixes Imgur albums and album thumbnails failing to load, and links with a title in them. | On |
| Fix load more comments | Fixes "load more comments" not loading further comments. | On |
| Fix markdown links | Shows markdown links that Reddit leaves as raw text, such as links to u/name, as links. | On |
| Fix media links | Opens reddit.com/media links as the Reddit-hosted image they wrap. | On |
| Fix Redgifs | Fixes Redgifs videos not loading. | On |
| Fix YouTube player | Fixes the built-in YouTube player, which showed a black screen and error code 0. | On |
| Keep typed text in view | Keeps the line being typed in view when writing a long reply. | On |
| Spoof client | Restores login and browsing by using another app's Reddit OAuth client. | On |

## Options

- **Spoof client** uses [RedReader](https://github.com/QuantumBadger/RedReader)'s public Reddit client by default,
  whose rate limit every RedReader user shares. If Reddit stops accepting it, set all three options to another
  "installed app" client: `OAuth client ID`, `Redirect URI` (the one registered for that ID) and `User-Agent` (the one
  that app sends). Logged out, the app then gets a new token by itself; a logged-in account's token belongs to the old
  client, so remove the account and add it again.
- **Fix Imgur** loads albums with the app's own Imgur client ID by default. If Imgur stops accepting it, set
  `Imgur client ID` to another.

## Known issues

- **Imgur uploads fail.** The app uploads through a RapidAPI proxy of Imgur's API that no longer accepts its key
  ("You are not subscribed to this API").
- **Synccit, new post notifications and downloadable themes are gone.** They need servers that no longer answer:
  synccit.com, and rif's rifnotifier.talklittle.com and redditthemes.com, so Enable golden platinum hides their settings,
  menu items and theme list. Themes from a folder (Theme, Enable developer mode) still work.
