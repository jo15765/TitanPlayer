# Titan Player

**Android TV IPTV player** built in Kotlin. Browse and play live TV from servers that support the Xtream Codes API.

---

## Disclaimer

**This is a player only.** Titan Player does not provide any streams, content, or subscription services. You must supply your own compatible IPTV source (server URL and credentials). The developers are not responsible for how you obtain or use stream sources.

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for the full text.

---

## How it works

1. **Login** — You enter your IPTV server URL (e.g. `http://server:port`), username, and password. The app validates the connection using the server’s `player_api.php` (Xtream Codes–compatible API).

2. **Setup** — You choose a quick setup (all channels) or an advanced flow where you pick countries and preferences. The app fetches live stream and category lists from the same API.

3. **Guide** — The main screen shows a channel list (by category) on the left and a video player on the right. Selecting a channel builds the stream URL in the form  
   `{baseUrl}/live/{username}/{password}/{stream_id}.ts`  
   and plays it with ExoPlayer (Media3). EPG (electronic program guide) is loaded per channel when available.

4. **Tech** — Single Activity flow (Login → Setup choice → Country/preferences → Player). Data is fetched via Retrofit; playback uses ExoPlayer with HLS/DASH support. No streams or credentials are bundled—everything is supplied by you at runtime.

---

## Requirements

- Android Studio (recent version)
- Android TV device or **TV** emulator
- Min SDK 24, target SDK 35
- Kotlin 1.9+, JDK 11+

---

## Build and run

1. Clone the repo and open the project in Android Studio.
2. Sync Gradle.
3. Run on an Android TV device or a **TV (1080p)** emulator.

```bash
# From project root (after sync)
./gradlew assembleDebug
```

---

## Project structure

| Path | Description |
|------|-------------|
| `app/src/main/java/.../` | `TitanPlayerApplication`, `data/` (API, repository, models), `ui/` (login, setup, player) |
| `app/src/main/res/` | Layouts, drawables, values (strings, themes, colors) |

---

## API compatibility

The app expects an Xtream Codes–style API:

- **Auth:** `GET player_api.php?username=...&password=...` (checks `user_info.auth == 1`).
- **Live streams:** `...&action=get_live_streams`.
- **Categories:** `...&action=get_live_categories`.
- **EPG:** `...&action=get_short_epg&stream_id=...` (optional).
- **Stream URL:** `{base}/live/{username}/{password}/{stream_id}.ts`.

---

*Titan Player — player only; you provide the streams.*
