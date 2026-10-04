package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Raw night palette (used by color schemes outside composition)
val NightDeepSpace = Color(0xFF050C12)
val NightDarkSurface = Color(0xFF0A1C22)
val NightSurfaceCard = Color(0xFF123139)
val NightSurfaceGlass = Color(0xF218343A)
val NightBorderGlow = Color(0xB35EEAD4)
val NightCyan = Color(0xFF62F5DD)
val NightGold = Color(0xFFFFD477)
val NightPurple = Color(0xFFE1ADFF)
val NightBlue = Color(0xFF7ADFFF)
val NightRose = Color(0xFFFF86A0)
val NightEmerald = Color(0xFF66E6A3)
val NightTextPrimary = Color(0xFFFFFFFF)
val NightTextSecondary = Color(0xFFD4E4E3)
val NightTextMuted = Color(0xFFA8BDBC)

// Day palette: deep caramel / parchment tones with near-black inks (darker, calmer than plain cream)
val DaySky = Color(0xFFCDB58A)
val DaySurface = Color(0xFFE2CFA8)
val DayCard = Color(0xFFD6BF93)
val DayGlass = Color(0xF2E8D7B4)
val DayBorder = Color(0xE05C4120)
val DayTextPrimary = Color(0xFF15100A)
val DayTextSecondary = Color(0xFF2E2416)
val DayTextMuted = Color(0xFF4A3B26)
val DayForest = Color(0xFF0E3B33)
val DayForestDark = Color(0xFF082721)
val DayGold = Color(0xFF6A3B05)
val DayWine = Color(0xFF7A1C33)
val DayTeal = Color(0xFF084A40)
val DayPurple = Color(0xFF47206B)
val DayBlue = Color(0xFF103B57)
val DayEmerald = Color(0xFF0F5530)
/** Text/icon colour that is always readable on a solid accent-coloured button. */
val DayOnAccent = Color(0xFFFFF6E3)

@Composable @ReadOnlyComposable
private fun night() = LocalGabbaiPalette.current.isNight

// Theme-aware accents: every screen reads these, so day mode is always readable.
val CosmicDeepSpace: Color @Composable @ReadOnlyComposable get() = if (night()) NightDeepSpace else DaySky
val CosmicDarkSurface: Color @Composable @ReadOnlyComposable get() = if (night()) NightDarkSurface else DaySurface
val CosmicSurfaceCard: Color @Composable @ReadOnlyComposable get() = if (night()) NightSurfaceCard else DayCard
val CosmicSurfaceGlass: Color @Composable @ReadOnlyComposable get() = if (night()) NightSurfaceGlass else DayGlass
val CosmicBorderGlow: Color @Composable @ReadOnlyComposable get() = if (night()) NightBorderGlow else DayBorder
val CosmicStardustCyan: Color @Composable @ReadOnlyComposable get() = if (night()) NightCyan else DayTeal
val CosmicCelestialGold: Color @Composable @ReadOnlyComposable get() = if (night()) NightGold else DayGold
val CosmicNebulaPurple: Color @Composable @ReadOnlyComposable get() = if (night()) NightPurple else DayPurple
val CosmicAuroraBlue: Color @Composable @ReadOnlyComposable get() = if (night()) NightBlue else DayBlue
val CosmicAlertRose: Color @Composable @ReadOnlyComposable get() = if (night()) NightRose else DayWine
val CosmicEmeraldSuccess: Color @Composable @ReadOnlyComposable get() = if (night()) NightEmerald else DayEmerald
val CosmicTextPrimary: Color @Composable @ReadOnlyComposable get() = if (night()) NightTextPrimary else DayTextPrimary
val CosmicTextSecondary: Color @Composable @ReadOnlyComposable get() = if (night()) NightTextSecondary else DayTextSecondary
val CosmicTextMuted: Color @Composable @ReadOnlyComposable get() = if (night()) NightTextMuted else DayTextMuted

/** Readable text colour for solid accent buttons in both modes. */
val CosmicOnAccent: Color @Composable @ReadOnlyComposable get() = if (night()) NightDeepSpace else DayOnAccent
