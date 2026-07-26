// Top-level build file: declares plugins used by subprojects without
// applying them here (each module opts in individually).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
