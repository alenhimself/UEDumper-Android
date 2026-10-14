This folder is populated by build-app.cmd (or the GitHub workflow) with
prebuilt MobileDumper-7 executables, one per ABI:

  assets/dumper/arm64-v8a/MobileDumper-7   <- builds/android/exe/release/arm64/MobileDumper-7
  assets/dumper/armeabi-v7a/MobileDumper-7  <- builds/android/exe/release/arm32/MobileDumper-7
  assets/dumper/x86_64/MobileDumper-7       <- builds/android/exe/release/x86_64/MobileDumper-7
  assets/dumper/x86/MobileDumper-7          <- builds/android/exe/release/x86/MobileDumper-7

Placeholders are never shipped: the build script aborts if a binary is missing.
