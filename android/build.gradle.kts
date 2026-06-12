plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Keep build output outside OneDrive to avoid file-locking issues
allprojects {
    layout.buildDirectory.set(File("C:/BuildCache/MusicTracker/${project.name}"))
}
