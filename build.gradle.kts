// Top-level build file for HealthTrend
// AGP 9.0 — Kotlin is built-in; do NOT apply org.jetbrains.kotlin.android

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
