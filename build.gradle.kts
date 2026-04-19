// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("org.sonarqube") version "5.0.0.4638" apply false
}

// SonarQube global configuration
sonar {
    properties {
        property("sonar.projectKey", "procrastiMATES")
        property("sonar.projectName", "procrastiMATES")
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java,src/androidTest/java")
        property("sonar.java.binaries", "**/classes")
        property("sonar.coverage.jacoco.xmlReportPaths", "**/coverage.xml")
    }
}