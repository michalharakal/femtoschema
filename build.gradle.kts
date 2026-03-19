plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "me.bechberger.util"
version = "0.2.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain {
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.json.schema.validator)
                implementation(libs.jackson.databind)
            }
        }
    }
}
