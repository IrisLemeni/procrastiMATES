plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("org.sonarqube")
    id("jacoco")
}

android {
    namespace = "com.example.procrastimates"
    compileSdk = 34

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    defaultConfig {
        applicationId = "com.example.procrastimates"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

// JaCoCo configuration for code coverage
jacoco {
    version = "0.8.10"
}

// Task to generate JaCoCo coverage report
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates JaCoCo code coverage report for debug unit tests."

    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
        html.outputLocation.set(file("${buildDir}/jacoco/index.html"))
    }

    fileTree(file("${buildDir}/intermediates/classes/debug")).apply {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*"
        )
    }.let { classDirectories.setFrom(it) }

    sourceDirectories.setFrom(file("src/main/java"))
    executionData.setFrom(fileTree(buildDir).include("outputs/coverage/*.ec", "outputs/unit_test_code_coverage/debugUnitTest/coverage.ec"))
}

dependencies {

    // AndroidX & UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Firebase BOM — governs all firebase-* versions below
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore.ktx)
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-storage")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.android.volley:volley:1.2.1")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Markdown rendering
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")

    // Charts & Calendar
    implementation(libs.material.calendar.view)
    implementation(libs.mpandroidchart)

    // Background work
    implementation("androidx.work:work-runtime:2.8.1")

    // Auth
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// SonarQube configuration
sonarqube {
    properties {
        property("sonar.projectKey", "procrastiMATES")
        property("sonar.projectName", "procrastiMATES")
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java,src/androidTest/java")
        property("sonar.java.binaries", "**/classes")
        property("sonar.coverage.jacoco.xmlReportPaths", "${buildDir}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.android.lint.report", "${buildDir}/reports/lint-results-debug.xml")
    }
}