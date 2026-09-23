<div align="center">

<img src="samples/play_store_screenshots/00_feature_graphic.png" alt="Net Speed" width="640"/>

# Net Speed

**Live internet speed in your status bar, and where your data actually goes.**

Per-app usage, data-limit forecasts, a floating overlay and a built-in speed test —
measured and stored entirely on your device.

<a href="https://play.google.com/store/apps/details?id=com.sultonuzdev.netspeed">
  <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="200"/>
</a>

</div>

---

## 🚀 Features

### Live speed
- **Status bar readout** — current speed rendered into the notification icon, so it is visible
  without pulling down the shade
- **Display modes** — download only, upload only, both directions, or a combined total. The choice
  drives the notification, the status bar icon, the floating overlay and the home screen together
- **Units** — Auto, Mbps, Kbps, MB/s or KB/s. Bit and byte units are genuinely converted, not
  relabelled
- **Live sparkline** of the last minute on the Speed screen
- **Connection details** — tap the network row for link speed, band, signal, IP and DNS, using
  only permissions that need no prompt

### Data usage
- **Per-app breakdown** — which apps used what, for today or the whole billing cycle
- **Foreground vs background split** per app, so you can see what an app moved while you were not
  using it
- **System-accurate totals** — read from Android's own accounting via `NetworkStatsManager`, so
  figures match Settings rather than being re-derived from sampling
- **Dual-SIM aware** — mobile usage is summed across subscribers
- **History** — 30-day table plus a weekly chart; tap any day for its totals and top apps
- **Data limit alerts** — set a mobile cap and a warning threshold, on your own billing cycle day.
  Each level notifies once per cycle
- **Usage forecast** — projects the cycle from the rate so far, so you know before you go over
- **Per-app limits** — an allowance for any single app, warned once per cycle
- **Roaming and background-data warnings** — the first the moment roaming starts, the second when
  an app moves a lot of data while you are not in it
- **Limit indicators** on history rows, amber at the threshold and red past the day's share

### Surfaces
- **Floating overlay** — draggable, always on top, showing speed plus latency and session total.
  Size, colour and background opacity are configurable; tap to open the app
- **Speed test** — download, upload, ping and jitter, run inline on the Speed screen
- **Two home screen widgets** — live speed, and data usage against your limit — plus a
  **Quick Settings tile** to toggle monitoring

### Behaviour
- **Foreground service** that survives the app being closed or swiped away
- **Auto-start after reboot** (optional), only if monitoring was running beforehand
- **Screen-off throttling** — sampling and redraws slow while the screen is off. Byte accounting is
  unaffected, since the counters are cumulative
- **Material You** dynamic colour on Android 12+, plus dark and light themes
- **Large screens** — content holds a readable column instead of stretching across a tablet, and
  the speed dial grows to use the extra height

## ✦ Screenshots

<div align="center">

<img src="samples/play_store_screenshots/01_img.png" width="165" alt="Speed test result"/>
<img src="samples/play_store_screenshots/02_img_1.png" width="165" alt="Per-app data usage"/>
<img src="samples/play_store_screenshots/03_img_2.png" width="165" alt="Billing cycle usage"/>
<img src="samples/play_store_screenshots/04_img_3.png" width="165" alt="30-day history"/>
<img src="samples/play_store_screenshots/05_img_4.png" width="165" alt="Settings"/>

**On a tablet**

<img src="samples/play_store_screenshots/tablet_10/01_tab_speed.png" width="200" alt="Speed screen on a tablet"/>
<img src="samples/play_store_screenshots/tablet_10/02_tab_usage.png" width="200" alt="Usage on a tablet"/>
<img src="samples/play_store_screenshots/tablet_10/03_tab_history.png" width="200" alt="History on a tablet"/>
<img src="samples/play_store_screenshots/tablet_10/04_tab_settings.png" width="200" alt="Settings on a tablet"/>

</div>

### Demo

A 79-second walkthrough — speed test, per-app usage, history and settings:
[`samples/netspeed_demo_1080p.mp4`](samples/netspeed_demo_1080p.mp4) (1920×1080).

Listing assets are generated rather than hand-assembled, so they can be rebuilt whenever the UI
changes:

```bash
cd samples
python3 make_play_screenshots.py            # phone + 7"/10" tablet slides, feature graphic
python3 make_demo_video.py demo_raw_phone.mp4   # frames a raw capture for YouTube
```

Raw device recordings are not committed — take a fresh one with
`adb shell screenrecord --bit-rate 16M /sdcard/demo.mp4`, pull it, and pass it to the script. The
still captures the listing images are built from (`samples/img*.png`, `samples/tab_*.png`) are.

Both read the app's own `res/font/manrope.ttf` and the palette from `theme/Color.kt`, so the
artwork and the product never drift apart. Title and description copy for the video lives in
[`samples/youtube.md`](samples/youtube.md).

---

## 🛠️ Tech Stack

- **Kotlin** + **Jetpack Compose**, Material 3
- **MVVM** with **Koin** for dependency injection
- **Room** for usage history (schema v4, with real migrations)
- **DataStore** for preferences
- **Foreground Service** for monitoring, **WindowManager** for the overlay
- **NetworkStatsManager** for accurate and per-app usage; **TrafficStats** for live speed
- **AppWidgetProvider** and **TileService** for the widget and tile
- minSdk 26 · targetSdk 36

## 🧩 Screen structure

Every screen file under `presentation/screens/<name>/` follows the same order, top to bottom:

| # | Function | Visibility | Role |
|---|---|---|---|
| 1 | `XScreen(modifier, viewModel = koinViewModel())` | public | The only entry point. Collects ViewModel state, runs lifecycle effects, and wires callbacks — **no layout here** |
| 2 | `XScreenContent(uiState, callbacks…, modifier)` | private | The whole UI, driven purely by parameters. Knows nothing about ViewModels, Koin or `LocalContext` |
| 3 | `XScreenContentPreview()` | private, `@Preview` | Renders `XScreenContent` inside `NetSpeedTheme` with hand-written sample state and no-op callbacks |
| 4 | helpers | private | Sub-composables and plain functions the content uses, in the order they are called |

```kotlin
@Composable
fun SpeedScreen(
    modifier: Modifier = Modifier,
    viewModel: SpeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SpeedScreenContent(
        uiState = uiState,
        onStart = { viewModel.startTest() },
        modifier = modifier
    )
}

@Composable
private fun SpeedScreenContent(
    uiState: SpeedUiState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) { /* layout */ }

@Preview(showBackground = true)
@Composable
private fun SpeedScreenContentPreview() {
    NetSpeedTheme(darkTheme = true) {
        SpeedScreenContent(uiState = SpeedUiState(/* sample */), onStart = {})
    }
}
```

Rules of thumb:

- UI state lives in a `data class` next to the ViewModel (`XUiState.kt`, or `contract/` when there
  is more than one). Everything the content needs to draw comes through that object.
- Callbacks are individual lambdas. When a screen has more than a handful, group them in a private
  `XActions` data class with no-op defaults so the preview can pass `XActions()` (see
  `SettingsScreen`).
- Anything that needs a `Context` or the ViewModel — starting a service, opening system settings,
  dialogs that read option lists off the ViewModel — is wired in `XScreen`, never in the content.
- Previews use a fixed `darkTheme = true` and populated sample data, so the Android Studio preview
  shows the screen as it looks with real content rather than an empty state.

## 🔑 Permissions

Granted automatically at install:

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Measure speed, read connection state |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep monitoring while closed. `specialUse` because Android 15 caps `dataSync` at ~6h/day |
| `RECEIVE_BOOT_COMPLETED` | Resume monitoring after a restart |
| `WAKE_LOCK` | Keep sampling while the screen is off |

Asked for at runtime — **nothing is requested on first launch**. Live speed comes from
`TrafficStats`, which needs no permission at all, so the app opens straight onto a working screen.
Each permission is then asked for by the feature that uses it, one at a time, with a plain-language
reason shown before the system dialog:

| Permission | Asked when | If declined |
|---|---|---|
| `POST_NOTIFICATIONS` (Android 13+) | You accept the card offering to put the speed in your status bar, or switch monitoring on in Settings | Everything in the app keeps working; Settings offers the way back |
| `READ_PHONE_STATE` | You open network details **on a mobile connection** — Wi-Fi never asks | The details sheet still opens, without the signal reading |

Granted by the user in system settings — the app cannot request these directly:

| Permission | Unlocks |
|---|---|
| **Usage access** (`PACKAGE_USAGE_STATS`) | Per-app usage and system-accurate totals |
| **Draw over other apps** (`SYSTEM_ALERT_WINDOW`) | The floating overlay |
| Battery optimisation exemption | Prevents aggressive OEM battery managers stopping the service. Opened as the system's battery-optimisation list rather than the restricted `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` dialog |

Without usage access the app still works, falling back to sampled figures — the Usage screen says
which mode it is in.

## 📱 Usage

1. Open the app; live speed is there immediately, with no permission to grant first
2. Accept the card at the top of the Speed screen to put that figure in your status bar, and grant
   **usage access** from the Usage tab for exact figures and per-app data
3. **Speed** — live speed, sparkline, ping and connection details
4. **Usage** — today's totals, your data cap, and the per-app breakdown
5. **History** — 30 days of daily usage; tap a day to see what used it
6. **Settings** — notification style and units, data limit and alerts, overlay, theme

Full privacy policy: <https://sultonuzdev.github.io/NetSpeed/privacy-policy.html>

## 🔒 Privacy

- **Usage and speed data stay on your device.** Nothing is uploaded, and there is no analytics or
  crash reporting
- **No ads, no tracking**
- **One exception:** the built-in speed test measures against Cloudflare's public endpoint
  (`speed.cloudflare.com`). No information about you is sent — the endpoints simply return or
  discard a number of bytes — but the request necessarily reveals your IP address to Cloudflare,
  as any speed test must. The test only runs when you start it, and its results are stored locally
- App names and icons in the per-app breakdown are read from your device's package manager, never
  from a network service

---

## 📄 Documents

- [Privacy policy](https://sultonuzdev.github.io/NetSpeed/privacy-policy.html) — what is stored, and the two special permissions
  ([source](docs/privacy-policy.html))
