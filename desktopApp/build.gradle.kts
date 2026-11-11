import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin{
    jvmToolchain(25)
}

dependencies {
    // kotlinx datetime
    implementation(libs.kotlinx.coroutines.jvm)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.datetime)
    implementation(libs.cmp.runtime)
    implementation(libs.cmp.foundation)
    implementation(libs.cmp.material3)
    implementation(libs.cmp.ui)
    implementation(compose.desktop.currentOs)
    implementation(project(":composeApp"))
}

compose.desktop {
    application {
        mainClass = "com.studyflow.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "StudyFlow"
            packageVersion = "1.0.0"
            modules("java.naming", "jdk.unsupported")
        }
    }
}
