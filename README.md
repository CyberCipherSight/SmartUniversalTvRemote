# Universal TV Remote

A lightweight, **ad-free and open-source Android TV remote** for phones with a built-in IR blaster.

The app opens as a normal remote for everyday use, while optional Advanced Mode exposes TV-profile selection, IR diagnostics, extra keys, and the guided **Find My TV** workflow.

> Current release: **v0.3.0**  
> Status: Panasonic is physically verified; Samsung, LG, Xiaomi and TCL profiles require broader hardware testing.

## Why I built this

I had a TV but not its remote. My phone had an IR blaster, so using the phone was the obvious fallback. The remote apps I tried from the Play Store worked, but many were cluttered with advertising.

That led to a simple idea: build a universal TV remote that does the basic job well, stays free of ads and tracking, and makes the implementation open for anyone to inspect, improve, or extend.

This project is intentionally local-first. It does not need an account, cloud service, analytics SDK, or Internet connection to transmit IR commands.

## Features

- **Simple Mode by default** — Power, Input, Home, Menu, Back, D-pad/OK, Volume, Mute and Channel controls.
- **Advanced Mode** — brand/model-family selection, profile details, diagnostics, number pad, HDMI shortcuts, color/media keys and supported app shortcuts.
- **Find My TV** — guided IR-profile discovery when the user does not know the exact model.
- **Adaptive layout** — unsupported buttons are hidden for the active profile.
- **IR carrier negotiation** — prefers the target carrier but checks the phone's advertised carrier ranges.
- **Saved profile** — once a working profile is found, the app opens directly as that remote.
- **No ads. No analytics. No tracking.**
- **No Internet permission.**

## Supported brands / IR families

| Brand | Profile | Protocol | Status |
|---|---|---|---|
| Panasonic | Viera | Panasonic/Kaseikyo-style, ~37 kHz | ✅ Physically verified |
| Samsung | Standard TV | Samsung32, 38 kHz | 🧪 Needs broader hardware testing |
| LG | Standard TV | NEC, 38 kHz | 🧪 Needs broader hardware testing |
| Xiaomi / Mi TV | IR 0x80 | NEC, 38 kHz | 🧪 Needs broader hardware testing |
| Xiaomi / Mi TV | 4S alternate | Partial NEC profile | 🧪 Needs broader hardware testing |
| TCL | Android TV / NIKAI | NIKAI 24-bit, 38 kHz | 🧪 Needs broader hardware testing |
| TCL | Roku / NEC Extended | Partial NEC Extended profile | 🧪 Needs broader hardware testing |

The project models **remote-code families**, not every screen-size SKU. Many TV models and sizes reuse the same remote family.

## Find My TV: what it actually detects

An IR blaster normally transmits one way; it cannot ask an IR-only television for its exact model number.

`Find My TV` therefore identifies the **working remote profile**:

1. Select a brand, or choose automatic mode.
2. Point the phone at the TV.
3. The app sends candidate Power commands one at a time.
4. Confirm when the TV reacts.
5. The working profile is saved.
6. The UI adapts to the commands available in that profile.

For remote control, identifying the working IR family is more useful than guessing the exact television SKU.

## Privacy

The current Android manifest declares only:

- `android.permission.TRANSMIT_IR`

It does **not** request `INTERNET`, location, contacts, storage, advertising ID, or analytics permissions. TV-profile preferences are stored locally on the device.

## Screenshots

Actual device screenshots will be added in [`docs/screenshots/`](docs/screenshots/README.md). The repository intentionally does not use fabricated UI screenshots.

## Download / APK

Prebuilt APKs are intentionally not committed to the source tree. They will be published manually under **GitHub Releases** after signing and device testing.

See [`releases/README.md`](releases/README.md).

## Build from source

Requirements:

- Android Studio
- JDK 17
- Android SDK 35
- A phone with Android consumer-IR hardware for real IR testing

Clone the repository and open the root folder in Android Studio, or build from the command line:

```bash
./gradlew assembleDebug
```

Project configuration:

- Application ID: `com.cybersight.panasonicremote`
- `minSdk`: 23
- `targetSdk`: 35
- Version: `0.3.0` (`versionCode 4`)

The application ID is currently retained from the original Panasonic prototype so existing test installations signed with the same key can be upgraded.

## Project structure

```text
app/src/main/java/com/cybersight/panasonicremote/
├── MainActivity.kt               # Simple/Advanced UI, setup and profile discovery
├── ir/
│   ├── IrProtocols.kt            # NEC, NEC Extended, Samsung32, NIKAI
│   ├── IrSignal.kt               # Protocol-independent signal types
│   ├── IrTransmitter.kt          # ConsumerIrManager wrapper and carrier handling
│   ├── PanasonicCodes.kt
│   └── PanasonicProtocol.kt      # Known-good Panasonic encoder
└── tv/
    ├── ModelCatalog.kt           # TV-family hints
    ├── TvProfile.kt
    └── TvProfiles.kt             # Brand/profile command maps
```

## Research and IR data provenance

See [`SOURCES.md`](SOURCES.md) for the references used while building the current profiles.

**Important:** IR code/protocol data can come from different upstream projects and community captures with different licenses. Before redistributing derived datasets commercially or expanding the database, verify the license/provenance of each imported source. The Apache-2.0 license in this repository applies to original project code and documentation; it does not override third-party rights or licenses.

## Contributing

Hardware coverage is the main bottleneck. Contributions are particularly useful when they include:

- TV brand and exact model
- region/country
- which profile worked or failed
- confirmed button mappings
- protocol, address/device and command values when available
- source/provenance for newly contributed IR data

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Security

Do not commit API keys, tokens, signing keys, Android keystores, service-account files, passwords, or private configuration. See [`SECURITY.md`](SECURITY.md).

## License

Original project code and documentation are licensed under the **Apache License 2.0**. See [`LICENSE`](LICENSE).
