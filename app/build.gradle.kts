plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "app.theone.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.theone.android"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 shrinking + obfuscation. Reflection-based classes (Firestore models) are kept in
            // src/main/keepRules/rules.keep.
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time on minSdk 24 (API < 26) - backported by core library desugaring.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.reorderable)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ktlint {
    // Pre-existing violations are grandfathered in here; new/changed lines are still checked.
    baseline = file("config/ktlint/baseline.xml")
}

// The org.jlleitschuh.gradle.ktlint plugin only auto-discovers Kotlin source sets via the
// classic Kotlin Gradle Plugin, which this module doesn't apply (AGP 9's built-in Kotlin support
// compiles app/src/**/*.kt instead - applying org.jetbrains.kotlin.android alongside it fails
// with "extension 'kotlin' already registered"). So ktlintCheck above only covers *.gradle.kts;
// this task runs the same ktlint CLI the plugin resolves, directly against app/src.
val ktlintCli by configurations.creating

dependencies {
    ktlintCli(libs.ktlint.cli)
}

val ktlintCheckSource by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Check app/src Kotlin source files with ktlint"
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt", "--baseline=config/ktlint/baseline.xml")
}

tasks.named("ktlintCheck") {
    dependsOn(ktlintCheckSource)
}

// So `./gradlew lint` catches style violations too, not just Android Lint's own checks.
tasks.named("lint") {
    dependsOn("ktlintCheck")
}
