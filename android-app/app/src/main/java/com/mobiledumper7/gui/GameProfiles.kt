package com.mobiledumper7.gui

/**
 * Game profiles mirrored from Dumper/Profile/CustomProfiles/Shared/.
 * The native dumper auto-picks a profile by package at runtime
 * (see GetUECustomProfiles() in Dumper/main_android.cpp), so the app
 * only has to supply the right package name — the chips below are
 * shortcuts that pre-fill known package names.
 */
data class GameProfile(
    val id: String,
    val label: String,
    val packages: List<String>,
    val hint: String,
)

object GameProfiles {
    val Auto = GameProfile(
        id = "auto",
        label = "Auto-detect",
        packages = emptyList(),
        hint = "Generic UE profile. Works for most games out of the box.",
    )
    val Pubg = GameProfile(
        id = "pubg",
        label = "PUBG Mobile",
        packages = listOf(
            "com.tencent.ig",
            "com.rekoo.pubgm",
            "com.pubg.imobile",
            "com.pubg.krmobile",
            "com.vng.pubgmobile",
        ),
        hint = "Pattern-scans GNames, then decrypts the pointer chain (PUBG.h).",
    )
    val DeltaForce = GameProfile(
        id = "deltaforce",
        label = "Delta Force",
        packages = listOf(
            "com.proxima.dfm",
            "com.garena.game.df",
            "com.tencent.tmgp.dfm",
        ),
        hint = "Decrypts XOR-encrypted UTF-8/UTF-16 name strings (DeltaForce.h).",
    )
    val Valorant = GameProfile(
        id = "valorant",
        label = "Valorant Mobile",
        packages = listOf(
            "com.tencent.tmgp.codev",
        ),
        hint = "Decrypts GNames + name chunks (Valorant.h).",
    )

    val all: List<GameProfile> = listOf(Auto, Pubg, DeltaForce, Valorant)

    private val packageToProfile: Map<String, GameProfile> =
        all.flatMap { profile -> profile.packages.map { pkg -> pkg to profile } }.toMap()

    /** Which native profile will handle this package. */
    fun guessProfile(packageName: String): GameProfile =
        packageToProfile[packageName.trim()] ?: Auto
}
