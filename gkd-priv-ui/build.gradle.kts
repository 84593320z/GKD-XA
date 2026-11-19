// Local Android UI fork of io.github.priv-kit:priv-ui:0.20.0.
// Keep priv-core/priv-shared pinned; do not also depend on the published priv-ui artifact.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "priv.kit.ui"
    buildFeatures { compose = true }
    sourceSets.named("main") {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        // AGP 9 built-in Kotlin requires AndroidSourceSet.kotlin, not java source dirs.
        kotlin.directories += listOf("src/androidMain/kotlin", "src/commonMain/kotlin")
        // Upstream uses identical XML for Compose text and synchronous notification text.
        // This Android-only fork resolves both through R, avoiding a second resource runtime.
        res.srcDirs("src/androidMain/res", "src/commonMain/composeResources")
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    api("io.github.priv-kit:priv-core:0.20.0")
    implementation("io.github.priv-kit:priv-shared:0.20.0")
    api(libs.miuix.ui)
    implementation(libs.miuix.icons)
    api(libs.compose.ui)
    implementation(libs.compose.animation)
    api("androidx.compose.foundation:foundation:1.12.1")
    api("androidx.compose.runtime:runtime:1.12.1")
    api("androidx.lifecycle:lifecycle-viewmodel:2.11.0")
    api(libs.androidx.lifecycle.service)
    implementation(libs.compose.activity)
    implementation("androidx.core:core-ktx:1.19.1")
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    compileOnly("androidx.annotation:annotation:1.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.4.21")
    testImplementation(libs.junit)
}
