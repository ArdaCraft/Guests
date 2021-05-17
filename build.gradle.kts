import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin") version "1.0.3"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "me.ardacraft"
version = "2.2-SNAPSHOT"
val versionString: String = version.toString()
val apiString: String = "7.3.0"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

sponge {
    apiVersion(apiString)
    plugin("guests") {
        loader(PluginLoaders.JAVA_PLAIN)
        displayName("Guests")
        mainClass("me.ardacraft.guests.Guests")
        description("Basic building restrictions ")
        links {
            homepage("https://ardacraft.me")
            source("https://github.com/ArdaCraft/Guests")
            issues("https://github.com/ArdaCraft/Guests/issues")
        }
        contributor("Dags") {
            description("Original author and developer")
        }
        contributor("Freshmilkymilk") {
            description("Developer")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

val javaTarget = 8 // Sponge targets a minimum of Java 8
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8" // Consistent source file encoding
        if (JavaVersion.current().isJava10Compatible) {
            release.set(javaTarget)
        }
    }
}

// Make sure all tasks which produce archives (jar, sources jar, javadoc jar, etc) produce more consistent output
tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

dependencies {
    implementation("com.github.dags-:CommandBus:2.0.3")
    implementation("com.github.dags-:Config:1.0.2")
}

tasks {
    shadowJar {
        relocate("me.dags.config", "me.ardacraft.guests.config")
        relocate("me.dags.commandbus", "me.ardacraft.guests.commandbus")

        archiveFileName.set("Guests-${versionString}-SpongeAPI-${apiString}.jar")
    }

    build {
        dependsOn(shadowJar)
    }
}