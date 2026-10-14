<p align="center">
  <img src="Images/icon.png" alt="ourlogo" width="120" height="120"/>
</p>

<p align="center">
  <b>GUI version of  <a href="https://github.com/MJx0/MobileDumper-7">@MJx0</a> MobileDumper-7 for Android</b>
</p>

<p align="center">
  <a href="https://github.com/alenhimself/UEDumper-Android/releases"><img src="https://img.shields.io/github/v/release/alenhimself/UEDumper-Android?style=for-the-badge" alt="Latest Release"/></a>
  <a href="https://github.com/alenhimself/UEDumper-Android/releases"><img src="https://img.shields.io/github/downloads/alenhimself/UEDumper-Android/total?style=for-the-badge" alt="Total Downloads"/></a>
  <a href="https://github.com/alenhimself/UEDumper-Android/blob/main/LICENSE"><img src="https://img.shields.io/github/license/alenhimself/UEDumper-Android?style=for-the-badge" alt="License"/></a>
</p>

# UEDumper-Android

Unreal engine dumper for **Android** with a Material3 GUI. Pick a game, tap dump, get a full Unreal Engine SDK as a `.zip`.

> This project is Android-only. The dumping core is [MJx0/MobileDumper-7](https://github.com/MJx0/MobileDumper-7) (`v1.2.1`, based on [Dumper-7](https://github.com/Encryqed/Dumper-7), memory backend [KittyMemoryEx](https://github.com/MJx0/KittyMemoryEx)). iOS targets from upstream are not included here.

## Download

Grab the APK from [**Releases**](https://github.com/alenhimself/UEDumper-Android/releases).

## Requirements

- Rooted device — KernelSU, Magisk, or APatch
- Android / Emulator 8.0+ (`minSdk 26`). Ships binaries for `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` — the app picks the right one automatically.
- The target game running in the foreground/background while dumping.

## Features

- **Game picker.** One-tap chips for known profiles, an installed-app dropdown, and manual package input. The native core auto-selects the matching profile at runtime.
- **Full dump options.** Memory access (`process_vm_readv` / `pread`), suspend-game toggle (SIGSTOP, recommended), and optional UE `.so` module dump.
- **Every output, always.** C++ SDK (see [UsingTheSDK](UsingTheSDK.md)), `.usmap` mappings, IDA mappings `.idmap`, Dumpspace `.json`, and the GObjects list — packed into one `.zip`.
- **Live console + result card.** Stream the dumper log in-app, then copy the path or share the `.zip`.
- **Full offset & layout auto-detection.** Nothing is pinned to an engine version or a hardcoded offset table; the UE binary analyzer locates `GObjects`/`GNames` from live memory.

## Supported games

| Profile | Packages |
| --- | --- |
| Auto-detect (generic) | any UE game |
| PUBG Mobile | `com.tencent.ig`, `com.rekoo.pubgm`, `com.pubg.imobile`, `com.pubg.krmobile`, `com.vng.pubgmobile` |
| Delta Force | `com.proxima.dfm`, `com.garena.game.df`, `com.tencent.tmgp.dfm` |
| Valorant Mobile | `com.tencent.tmgp.codev` |
| Farlight 84 | Currently Unsupported Working On it  `Dumper/Profile/CustomProfiles/Shared/Farlight84.h` |

Most UE games work with Auto-detect and need no profile at all.

## Usage

1. Install the APK, launch the app, grant root when prompted.
2. Pick the target game (chip, dropdown, or type the package).
3. Keep Suspend ON, choose the memory type (`process_vm_readv` first, `pread` as fallback).
4. Tap **Dump selected game** and wait — do not close the game.
5. Share the `.zip` from the result card.

## Custom game profiles

A profile is only needed when a game encrypts its UE internals (GNames pointers, name strings, object items).

1. Create a header in `Dumper/Profile/CustomProfiles/Shared/`, e.g. `MyGame.h`:

```cpp
#pragma once
#include "../../IProfile.h"

class MyGameProfile : public IProfile
{
public:
    std::vector<std::string> GetSupportedGames() const override
    {
        return {"com.example.mygame"};
    }

    // Override only what the game encrypts — leave everything else out.
};
```

Available overrides (`Dumper/Profile/IProfile.h`): `GetGObjects` / `DecryptGObjects`, `GetGNames` / `DecryptGNames`, `DecryptNameChunk`, `DecryptNameEntry`, `DecryptUTF8` / `DecryptUTF16` / `DecryptUTF32`, `ResolveGObjectsLayout`, `ResolveGNamesLayout`, `OverrideSettings`, `OverrideDecryptCallbacks`. See `PUBG.h` (pattern-scan + pointer-chain GNames), `DeltaForce.h` (XOR strings), `Valorant.h` (encrypted chunks) for examples.

2. Register it in `Dumper/main_android.cpp` → `GetUECustomProfiles()`:

```cpp
UECustomProfiles.push_back(std::make_shared<MyGameProfile>());
```

3. Rebuild (below). The app needs no change — it passes the package name and the core picks your profile. Optionally add a chip in `android-app/.../GameProfiles.kt`.

## Building from source

Prerequisites: JDK 21, Android SDK (`platforms;android-34`, `build-tools;34.0.0`), **NDK r27+** (r25 cannot compile this source — it needs C++20 `std::same_as` and friends), CMake 3.22+, Ninja, Gradle 8.7.

On Windows, one command does everything (native exes → app assets → debug APK):

```
build-app.cmd
```

Manual equivalent: `cmake --preset android-<abi>-exe && cmake --build --preset android-<abi>-exe` for `arm64`, `armeabi-v7a`, `x86_64`, `x86`, copy each binary to `android-app/app/src/main/assets/dumper/<abi>/MobileDumper-7`, then `gradle -p android-app :app:assembleDebug`. CI (`.github/workflows/android-app.yml`) does the same on every push.

Clone with submodules:

```
git clone --recurse-submodules https://github.com/alenhimself/UEDumper-Android.git
```

## Project structure

```
Dumper/                 Dumping core (UE analyzer, generators, memory, profiles)
libs/                   KittyMemoryEx submodule
android-app/            Material3 GUI (libsu root shell, game picker, console)
  design/               Icon source art
build-app.cmd           One-shot Windows build (native + APK)
.github/workflows/     CI: native exes + debug APK artifacts
```

## Credits & license

- [MJx0/MobileDumper-7](https://github.com/MJx0/MobileDumper-7), [Dumper-7](https://github.com/Encryqed/Dumper-7), [UEDumper](https://github.com/Spuckwaffel/UEDumper), [KittyMemoryEx](https://github.com/MJx0/KittyMemoryEx)
- MIT — see [LICENSE](LICENSE).
