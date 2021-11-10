/*
 * CodeScan for IntelliJ IDEA
 * Copyright (C) 2015-2021 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.telemetry

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.sonarlint.intellij.common.util.SonarLintUtils
import org.sonarlint.intellij.config.Settings
import org.sonarlint.intellij.config.global.ServerConnection
import org.sonarlint.intellij.config.global.SonarLintGlobalSettings
import org.sonarlint.intellij.core.NodeJsManager
import org.sonarlint.intellij.core.ProjectBindingManager
import org.sonarlint.intellij.core.SonarLintEngineManager
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails
import org.sonarsource.sonarlint.core.telemetry.TelemetryClientAttributesProvider
import java.util.Arrays
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Collectors

class TelemetryClientAttributeProviderImpl : TelemetryClientAttributesProvider {

    override fun usesConnectedMode(): Boolean {
        return isAnyProjectConnected()
    }

    override fun useSonarCloud(): Boolean {
        return isAnyProjectConnectedToCodeScanCloud()
    }

    override fun nodeVersion(): Optional<String> {
        val nodeJsManager = SonarLintUtils.getService(NodeJsManager::class.java)
        return Optional.ofNullable(nodeJsManager.nodeJsVersion?.toString())
    }

    override fun devNotificationsDisabled(): Boolean {
        return isDevNotificationsDisabled()
    }

    override fun getNonDefaultEnabledRules(): Collection<String> {
        val rules = Settings.getGlobalSettings().rules
            .filter(SonarLintGlobalSettings.Rule::isActive)
            .map(SonarLintGlobalSettings.Rule::getKey)
            .toSet()
        val defaultEnabledRuleKeys = defaultEnabledRuleKeys()
        return rules.minus(defaultEnabledRuleKeys)
    }

    override fun getDefaultDisabledRules(): Collection<String> {
        return Settings.getGlobalSettings().rules
            .filter { rule: SonarLintGlobalSettings.Rule -> !rule.isActive }
            .map(SonarLintGlobalSettings.Rule::getKey)
            .toSet()
    }

    override fun additionalAttributes() = emptyMap<String, Any>()

    private fun defaultEnabledRuleKeys(): Set<String> {
        val engineManager = SonarLintUtils.getService(SonarLintEngineManager::class.java)
        return engineManager.standaloneEngine.allRuleDetails.stream()
            .filter { obj: StandaloneRuleDetails -> obj.isActiveByDefault }
            .map { obj: StandaloneRuleDetails -> obj.key }
            .collect(Collectors.toSet())
    }

    companion object {

        private fun isAnyProjectConnected(): Boolean =
            isAnyOpenProjectMatch { p: Project -> Settings.getSettingsFor(p).isBindingEnabled }

        private fun isAnyProjectConnectedToCodeScanCloud(): Boolean = isAnyOpenProjectMatch { p: Project ->
            val bindingManager = SonarLintUtils.getService(p, ProjectBindingManager::class.java)
            bindingManager.tryGetServerConnection()
                .filter { obj: ServerConnection -> obj.isCodeScanCloud }
                .isPresent
        }

        private fun isDevNotificationsDisabled(): Boolean = Settings.getGlobalSettings().serverConnections.stream()
            .anyMatch { obj: ServerConnection -> obj.isDisableNotifications }

        private fun isAnyOpenProjectMatch(predicate: Predicate<Project>): Boolean {
            val projectManager = ProjectManager.getInstance()
            val openProjects = projectManager.openProjects
            return Arrays.stream(openProjects).anyMatch(predicate)
        }

    }
}
