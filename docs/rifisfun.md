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
| Fix load more comments | Fixes "load more comments" not loading further comments. | On |
| Fix YouTube player | Fixes the built-in YouTube player, which showed a black screen and error code 0. | On |
| Spoof client | Restores login and browsing by using another app's Reddit OAuth client. | On |

## Options

- **Spoof client** uses [RedReader](https://github.com/QuantumBadger/RedReader)'s public Reddit client by default,
  whose rate limit every RedReader user shares. If Reddit stops accepting it, set all three options to another
  "installed app" client: `OAuth client ID`, `Redirect URI` (the one registered for that ID) and `User-Agent` (the one
  that app sends). Logged out, the app then gets a new token by itself; a logged-in account's token belongs to the old
  client, so remove the account and add it again.

## Known issues

- **"Load more comments" loads at most about 20 comments per tap in long threads, and a new row stays for the rest.**
  Reddit refuses the app's request for them, so each comes from a page of its own; replies to a comment usually come
  from one page of their parent.
