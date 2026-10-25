# ANIMEVERSE Launcher (Android TV, Android 14)

The theme artwork is bundled (`app/src/main/assets/wallpaper.jpg`) and already contains the painted
menu and app tiles. The launcher draws only a live clock; invisible focus areas sit exactly on each
painted tile and menu row, so the remote shows a warm-white focus ring and launches the real app.

## Build
Push to GitHub: Actions builds the APK and publishes `animeverse.apk` under Releases.

## Install on the TV
Downloader app > type the release link for `animeverse.apk` > Install.
Uninstall any older ANIMEVERSE first (each build is signed with a fresh debug key).

## Remote
| Action | Result |
|---|---|
| Arrows / OK | Move focus / open app |
| Dock tiles | Netflix, YouTube, Disney+, Prime Video, Apple TV, Max, Crunchyroll, Spotify |
| More Apps / Apps | Full list of every installed app |
| TV Shows / Movies / Anime | Netflix / Prime Video / Crunchyroll |
| Settings, search icon | TV settings, voice search |
| Hold OK on Home | Ask Android to make ANIMEVERSE the default Home app |
| MENU button | Clean memory (kills cached background apps, shows MB freed) |
| Back | Does nothing on Home |

An app that is not installed shows "not installed" and opens its Play Store page if the TV has one.

## Changing the artwork
Any replacement `wallpaper.jpg` must keep the same layout (menu left, dock bottom, same positions) or the focus
areas will not line up. Coordinates live in `MainActivity.kt` (design space 1672 x 941).

## Cleanup (reversible, optional, needs adb)
`scripts\list-packages.bat`, edit `scripts\debloat.txt`, `scripts\debloat.bat`. Undo with `scripts\restore.bat`.
