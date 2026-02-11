package com.healthtrend.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color definitions for HealthTrend theme.
 * Palette derived from the brand logo: Teal (#14B8A6), Deep Blue (#1E3A8A), Coral (#F97373).
 * Severity colors are defined in the Severity enum — these are theme palette tokens.
 * See [com.healthtrend.app.data.model.Severity] for severity-specific colors.
 */

// ── Light theme tokens ──────────────────────────────────────────

// Primary palette — Teal (dominant logo color)
val PrimaryLight = Color(0xFF14B8A6)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFB2F5EA)
val OnPrimaryContainerLight = Color(0xFF00382E)

// Secondary palette — Deep Blue (logo accent)
val SecondaryLight = Color(0xFF1E3A8A)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD6E4FF)
val OnSecondaryContainerLight = Color(0xFF0D1B3E)

// Tertiary palette — Coral (logo highlight)
val TertiaryLight = Color(0xFFF97373)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDAD6)
val OnTertiaryContainerLight = Color(0xFF410002)

// Error palette
val ErrorLight = Color(0xFFB00020)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF93000A)

// Background / Surface — warm mint-white
val BackgroundLight = Color(0xFFFAFDFB)
val OnBackgroundLight = Color(0xFF191C1B)
val SurfaceLight = Color(0xFFFAFDFB)
val OnSurfaceLight = Color(0xFF191C1B)
val SurfaceVariantLight = Color(0xFFDBE5E0)
val OnSurfaceVariantLight = Color(0xFF3F4945)
val OutlineLight = Color(0xFF6F7975)
val OutlineVariantLight = Color(0xFFBFC9C4)
val SurfaceContainerLowLight = Color(0xFFF1F5F2)
val SurfaceContainerLight = Color(0xFFEBF0ED)
val SurfaceContainerHighLight = Color(0xFFE6EBE8)

// ── Dark theme tokens ───────────────────────────────────────────

// Primary palette — lighter teal for dark surfaces
val PrimaryDark = Color(0xFF5EEAD4)
val OnPrimaryDark = Color(0xFF003830)
val PrimaryContainerDark = Color(0xFF005048)
val OnPrimaryContainerDark = Color(0xFFB2F5EA)

// Secondary palette — lighter blue for dark surfaces
val SecondaryDark = Color(0xFFADC6FF)
val OnSecondaryDark = Color(0xFF0D2B6B)
val SecondaryContainerDark = Color(0xFF1B3B85)
val OnSecondaryContainerDark = Color(0xFFD6E4FF)

// Tertiary palette — softer coral for dark surfaces
val TertiaryDark = Color(0xFFFFB4AB)
val OnTertiaryDark = Color(0xFF690005)
val TertiaryContainerDark = Color(0xFF93000A)
val OnTertiaryContainerDark = Color(0xFFFFDAD6)

// Error palette
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Background / Surface — deep charcoal-green
val BackgroundDark = Color(0xFF191C1B)
val OnBackgroundDark = Color(0xFFE1E3E0)
val SurfaceDark = Color(0xFF191C1B)
val OnSurfaceDark = Color(0xFFE1E3E0)
val SurfaceVariantDark = Color(0xFF3F4945)
val OnSurfaceVariantDark = Color(0xFFBFC9C4)
val OutlineDark = Color(0xFF89938E)
val OutlineVariantDark = Color(0xFF3F4945)
val SurfaceContainerLowDark = Color(0xFF1F2422)
val SurfaceContainerDark = Color(0xFF252A27)
val SurfaceContainerHighDark = Color(0xFF303532)
