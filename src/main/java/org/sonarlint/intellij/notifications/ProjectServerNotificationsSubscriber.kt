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
package org.sonarlint.intellij.notifications

import com.intellij.ProjectTopics
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.ui.UIUtil
import icons.SonarLintIcons
import org.sonarlint.intellij.common.ui.SonarLintConsole
import org.sonarlint.intellij.common.util.SonarLintUtils.getService
import org.sonarlint.intellij.config.Settings
import org.sonarlint.intellij.config.global.ServerConnection
import org.sonarlint.intellij.config.global.SonarLintGlobalSettings
import org.sonarlint.intellij.config.global.wizard.ServerConnectionWizard
import org.sonarlint.intellij.config.project.SonarLintProjectState
import org.sonarlint.intellij.core.ProjectBindingManager
import org.sonarlint.intellij.core.ServerNotificationsService
import org.sonarlint.intellij.messages.GlobalConfigurationListener
import org.sonarlint.intellij.messages.ProjectConfigurationListener
import org.sonarlint.intellij.notifications.SonarLintProjectNotifications.SERVER_NOTIFICATIONS_GROUP
import org.sonarlint.intellij.telemetry.SonarLintTelemetry
import org.sonarsource.sonarlint.core.client.api.common.NotificationConfiguration
import org.sonarsource.sonarlint.core.client.api.notifications.LastNotificationTime
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotification
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotificationListener
import java.time.ZonedDateTime

class ProjectServerNotificationsSubscriber : Disposable {
  private val project: Project
  private val notificationsService: ServerNotificationsService
  private var eventListener: EventListener? = null
  private val notificationTime: ProjectNotificationTime

  constructor(project: Project) : this(project, ServerNotificationsService.get())

  constructor(project: Project, notificationsService: ServerNotificationsService) {
    this.project = project
    this.notificationsService = notificationsService
    notificationTime = ProjectNotificationTime()
  }

  fun start() {
    register()
    val busConnection = project.messageBus.connect()
    busConnection.subscribe(ProjectConfigurationListener.TOPIC, ProjectConfigurationListener {
      // always reset notification date, whether bound or not
      val projectState = getService(project, SonarLintProjectState::class.java)
      projectState.lastEventPolling = ZonedDateTime.now()
      ApplicationManager.getApplication().executeOnPooledThread {
        register()
      }
    })
    busConnection.subscribe(GlobalConfigurationListener.TOPIC, object : GlobalConfigurationListener.Adapter() {
      override fun applied(settings: SonarLintGlobalSettings) {
        ApplicationManager.getApplication().executeOnPooledThread {
          register()
        }
      }
    })
    busConnection.subscribe(ProjectTopics.MODULES, object : ModuleListener {
      override fun moduleAdded(project: Project, module: Module) {
        register()
      }

      override fun moduleRemoved(project: Project, module: Module) {
        register()
      }
    })
  }

  @Synchronized private fun register() {
    unregister()
    getService(project, ProjectBindingManager::class.java).tryGetServerConnection()
      .filter { !it.isDisableNotifications }
      .ifPresent {
        eventListener = EventListener(it.isCodeScanCloud, it.name)
        try {
          if (notificationsService.isSupported(it)) {
            getService(project, ProjectBindingManager::class.java).uniqueProjectKeys.forEach { projectKey ->
              notificationsService.register(createConfiguration(projectKey, it))
            }
          }
        } catch (e: Exception) {
          SonarLintConsole.get(project)
            .error("Cannot register for server notifications. The server might be unreachable", e)
        }
    }
  }

  override fun dispose() {
    unregister()
  }

  private fun unregister() {
    if (eventListener != null) {
      notificationsService.unregister(eventListener)
      eventListener = null
    }
  }

  private fun createConfiguration(projectKey: String, server: ServerConnection): NotificationConfiguration {
    return NotificationConfiguration(eventListener, notificationTime, projectKey, { server.endpointParams }, { server.httpClient })
  }

  /**
   * Read and save directly from the mutable object.
   * Any changes in the project settings will affect the next request.
   */
  private inner class ProjectNotificationTime : LastNotificationTime {
    override fun get(): ZonedDateTime {
      val projectState = getService(project, SonarLintProjectState::class.java)
      var lastEventPolling = projectState.lastEventPolling
      if (lastEventPolling == null) {
        lastEventPolling = ZonedDateTime.now()
        projectState.lastEventPolling = lastEventPolling
      }
      return lastEventPolling!!
    }

    override fun set(dateTime: ZonedDateTime) {
      val projectState = getService(project, SonarLintProjectState::class.java)
      val lastEventPolling = projectState.lastEventPolling
      if (lastEventPolling != null && dateTime.isBefore(lastEventPolling)) {
        // this can happen if the settings changed between the read and write
        return
      }
      projectState.lastEventPolling = dateTime
    }
  }

  /**
   * Simply displays the events and discards it
   */
  private inner class EventListener constructor(private val isSonarCloud: Boolean, private val connectionName: String) : ServerNotificationListener {

    override fun handle(serverNotification: ServerNotification) {
      val telemetry = getService(SonarLintTelemetry::class.java)
      val category = serverNotification.category()
      telemetry.devNotificationsReceived(category)
      val label = if (isSonarCloud) "SonarCloud" else "SonarQube"
      val notif = SERVER_NOTIFICATIONS_GROUP.createNotification(
        "<b>$label Notification</b>",
        serverNotification.message(),
        NotificationType.INFORMATION,
        null)
      notif.icon = if (isSonarCloud) SonarLintIcons.ICON_SONARCLOUD_16 else SonarLintIcons.ICON_SONARQUBE_16
      notif.isImportant = true
      notif.addAction(OpenInServerAction(label, serverNotification.link(), category))
      notif.addAction(ConfigureNotificationsAction(connectionName))
      notif.notify(project)
    }
  }

  private class OpenInServerAction(serverLabel: String, private val link: String, private val category: String) : NotificationAction("Open in $serverLabel") {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
      val telemetry = getService(SonarLintTelemetry::class.java)
      telemetry.devNotificationsClicked(category)
      BrowserUtil.browse(link)
      notification.hideBalloon()
    }
  }

  private inner class ConfigureNotificationsAction(private val connectionName: String) : NotificationAction("Configure") {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
      WindowManager.getInstance().getFrame(e.project) ?: return
      UIUtil.invokeLaterIfNeeded {
        val connectionToEdit = Settings.getGlobalSettings().serverConnections.find { it.name == connectionName }
        if (connectionToEdit != null) {
          val wizard = ServerConnectionWizard.forNotificationsEdition(connectionToEdit)
          if (wizard.showAndGet()) {
            val editedConnection = wizard.connection
            val serverConnections = Settings.getGlobalSettings().serverConnections
            serverConnections[serverConnections.indexOf(connectionToEdit)] = editedConnection
            register()
          }
        } else if (e.project != null) {
          SonarLintConsole.get(e.project!!).error("Unable to find connection with name: $connectionName")
          notification.expire()
        }
      }
    }
  }
}
