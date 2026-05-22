# Lumiris IPTV

A landscape-only IPTV Android app for phones and Android TV, built with Kotlin, Jetpack Compose, Media3 ExoPlayer, MVVM + Clean Architecture, and Kotlin Coroutines/Flow.

## Features

- **Landscape-locked, fullscreen immersive** — works identically on phones and Android TV.
- **Custom M3U parser** — supports the user's `=== Category ===` delimiter format and standard `#EXTINF` lines, robust to missing/invalid logos.
- **Media3 ExoPlayer** — auto-detects DASH (`.mpd`), HLS (`.m3u8`), progressive (`.mp4`) and Google Drive streams, with optional ClearKey DRM for DASH.
- **Google Drive helper** — converts share links (`drive.google.com/file/d/<ID>/view`) to direct streaming URLs.
- **Splash → Main flow** — background fetch on splash, auto-plays the first channel of the first category.
- **Manual refresh** — top-right refresh icon re-fetches the playlist (cache-bypass) and shows a subtle inline indicator. Player continuity is preserved when the currently-watched channel is still present.
- **Dark, modern UI** — vertical scrolling categories with horizontal channel cards. Missing-logo cards show a clean placeholder TV icon and remain clickable.

## Architecture

```
com.suzunei.lumirisiptv
├── data
│   ├── model       (DTO / raw model)
│   ├── parser      (M3UParser — handles === Category === and #EXTINF)
│   ├── remote      (Retrofit M3UApi, scalar String response)
│   ├── repository  (PlaylistRepository — cache-bypass refresh)
│   └── util        (GoogleDriveUrlHelper, StreamTypeDetector)
├── domain
│   └── model       (Channel, Category)
├── ui
│   ├── splash      (SplashActivity)
│   ├── main        (MainActivity, MainViewModel, MainScreen)
│   ├── components  (ChannelCard, CategoryRow)
│   ├── player      (PlayerHolder, MediaItemFactory)
│   └── theme       (Color, Theme, Type)
└── util
```

## Playlist Source

`https://raw.githubusercontent.com/hairunnizam21/myiptv-playlist/refs/heads/main/animedantv.m3u`

## Build

```bash
./gradlew :app:assembleDebug
```

Outputs `app/build/outputs/apk/debug/app-debug.apk`.
