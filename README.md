# ANIMEVERSE Launcher (Android TV, Android 14)

Zero-dependency launcher: Home dock (Apple TV style focus), All Apps, one-tap RAM clean, no ads, no network calls.

## 1. Get the APK (pick one)

**A. GitHub (no Android Studio needed)**
1. Push this folder to a GitHub repo.
2. Actions tab > "Build APK" > download artifact `animeverse-apk`. Rename the file to `animeverse.apk`.

**B. Android Studio**: open this folder, Build > Build APK(s). Use the release variant (`app-release.apk`).

## 2. Wallpaper (important)

The wallpaper must be the **clean artwork without the menu/dock baked in**. The screenshot you sent already has the UI painted on, so the launcher UI would double up on it.
Export the art only, 16:9, 1920x1080 (or 2560x1440) JPEG, name it `wallpaper.jpg`, and either:
- drop it next to `install.bat` (project root) before installing, or
- drop it in `app/src/main/assets/` before building, or
- later: `adb push wallpaper.jpg /sdcard/Android/data/com.animeverse.launcher/files/wallpaper.jpg` (no rebuild).

With no file present you get a themed gradient so the launcher still works.

## 3. Install over Wi-Fi (no cable)

Install Android platform-tools on Windows and add `adb` to PATH. TV and PC on the same network.

TV: Settings > Device Preferences > About > click **Build** 7 times. Then Developer options > turn on **Wireless debugging** (or "ADB debugging" / "Network debugging" on older firmware).

- Pairing code flow (Android 14): run `scripts\pair.bat`, enter the pairing IP:PORT and code the TV shows (once).
- Then run `scripts\install.bat`, enter the IP:PORT shown on the main Wireless debugging screen.

The script installs, pushes the wallpaper if present, and asks Android to make ANIMEVERSE the Home app. If the TV ignores that, press Home on the remote, pick ANIMEVERSE, tap **Always**.

## 4. Remote control

| Action | Result |
|---|---|
| Arrows | Move focus (tile lifts 10%) |
| OK | Open app (RAM is cleaned in the background first) |
| Hold OK on All Apps tile | Pin / unpin on Home (first 8 pinned show) |
| Hold OK on Home tile | Remove from Home |
| Clean (left menu) | Kills cached background apps, shows MB freed |
| Back | Does nothing on Home |

Default pins: Netflix, YouTube, Disney+, Prime Video, Apple TV, Max, Crunchyroll, Spotify. Only the ones actually installed appear.

## 5. Cleanup / debloat (reversible)

1. `scripts\list-packages.bat` then read `packages-system.txt`.
2. Edit `scripts\debloat.txt`, uncomment only what you recognise.
3. `scripts\debloat.bat` disables (does not delete). `scripts\restore.bat` undoes it.

Do not disable the stock launcher until ANIMEVERSE works and is your Home. Keep `restore.bat` handy.

## Why it is light
- No libraries, no Compose, no image loader: roughly 100 KB APK.
- Wallpaper decoded once, capped near 1080p, freed when another app is in front.
- No animations except a 110 ms focus scale; no window transitions.
- No network permissions beyond reading connection state; nothing to fetch ads.

## Limits (be aware)
- This replaces the launcher UI only. Ads/promos inside other apps, or vendor services that the firmware keeps running, need the debloat step and may be locked by VEERSUI.
- Clean can only kill processes Android deems safe; it won't beat a TV that is simply out of RAM.
- Not compile-tested in the build sandbox (no Android SDK). If the first GitHub Actions run errors, send me the log.
