val sonarlintCoreVersion: String by project
val intellijBuildVersion: String by project
val apacheCommonsLang3: String by project

intellij {
    version.set(intellijBuildVersion)
}

dependencies {
    implementation("org.sonarsource.sonarlint.core:sonarlint-core:$sonarlintCoreVersion")
    implementation("org.apache.commons:commons-lang3:$apacheCommonsLang3")
}
