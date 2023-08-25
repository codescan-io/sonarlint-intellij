import java.net.URL
import java.util.concurrent.Executors

plugins {
    id("org.jetbrains.intellij")
    id("com.github.hierynomus.license")
    kotlin("jvm")
}

group = "org.sonarsource.sonarlint.intellij.its"
description = "ITs for CodeScan IntelliJ"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "17"

val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "17"

repositories {
    mavenCentral()
    maven("https://repox.jfrog.io/repox/sonarsource")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

val remoteRobotVersion = "0.11.18"

dependencies {
    testImplementation("org.sonarsource.orchestrator:sonar-orchestrator:3.35.1.2719") {
        exclude(group = "org.slf4j", module = "log4j-over-slf4j")
    }
    testImplementation("org.sonarsource.slang:sonar-scala-plugin:1.8.3.2219")
    testImplementation("org.sonarsource.sonarqube:sonar-ws:8.5.1.38104")
    testImplementation("com.intellij.remoterobot:remote-robot:$remoteRobotVersion")
    testImplementation("com.intellij.remoterobot:remote-fixtures:$remoteRobotVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}

license {
    mapping(
        mapOf(
            "kt" to "SLASHSTAR_STYLE"
        )
    )
    // exclude file from resources (workaround for https://github.com/hierynomus/license-gradle-plugin/issues/145)
    exclude("**.xml")
    strictCheck = true
}

tasks.downloadRobotServerPlugin {
    version.set(remoteRobotVersion)
}

val ijVersion: String by project

intellij {
    version.set(if (project.hasProperty("ijVersion")) ijVersion else rootProject.intellij.version.get())
    pluginName.set("sonarlint-intellij-its")
    updateSinceUntilBuild.set(false)
    if (!project.hasProperty("slPluginDirectory")) {
        plugins.set(listOf(rootProject))
    }
}

tasks.runIdeForUiTests {
    systemProperty("robot-server.port", "8082")
    systemProperty("sonarlint.telemetry.disabled", "true")
    systemProperty("jb.privacy.policy.text", "<!--999.999-->")
    systemProperty("jb.consents.confirmation.enabled", "false")
    systemProperty("eap.require.license", "true")
    jvmArgs = listOf("-Xmx1G")
}

open class RunIdeForUiTestAsyncTask : DefaultTask() {
    @TaskAction
    fun startAsync() {
        val es = Executors.newSingleThreadExecutor()
        es.submit {
            project.tasks.runIdeForUiTests.get().exec()
        }
    }
}

val runIdeForUiTestsAsync by tasks.register<RunIdeForUiTestAsyncTask>("runIdeForUiTestsAsync") {
    dependsOn(tasks.runIdeForUiTests.get().dependsOn)
    doFirst {
        if (project.hasProperty("slPluginDirectory")) {
            copy {
                from(project.property("slPluginDirectory"))
                into(tasks.runIdeForUiTests.get().pluginsDir.get())
            }
        }
    }
}

open class WaitRobotServerTask : DefaultTask() {
    var port = "8082"
    var totalTimeSeconds = 240
    var retryPeriodSeconds = 5

    @TaskAction
    fun waitService() {
        var remainingTime = totalTimeSeconds
        println("Waiting for robot server on port $port")
        while (remainingTime > 0) {
            try {
                URL("http://localhost:$port").openStream()
                println("Robot server is running!")
                return
            } catch (ignored: Exception) {
                Thread.sleep(retryPeriodSeconds * 1000L)
                remainingTime -= retryPeriodSeconds
            }
        }
        throw RuntimeException("Robot server is unreachable")
    }
}

val waitRobotServer by tasks.register<WaitRobotServerTask>("waitRobotServer") {
    mustRunAfter(runIdeForUiTestsAsync)
}

tasks.test {
    mustRunAfter(waitRobotServer)
}

val runIts by tasks.register("runIts") {
    dependsOn(runIdeForUiTestsAsync, waitRobotServer, tasks.test)
}

tasks.check {
    dependsOn(runIts)
}
