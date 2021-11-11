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
package org.sonarlint.intellij.notifications;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import javax.swing.event.HyperlinkEvent;

import org.jetbrains.annotations.NotNull;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.config.global.ServerConnection;
import org.sonarlint.intellij.config.global.ServerConnectionMgmtPanel;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;

public class SonarLintProjectNotifications {
  private static final NotificationGroup BINDING_PROBLEM_GROUP = NotificationGroup.balloonGroup("CodeScan: Server Binding Errors");
  private static final NotificationGroup UPDATE_GROUP = NotificationGroup.balloonGroup("CodeScan: Configuration update");
  // this constructor invokation has to remain in Java code, else the Kotlin overload with default arguments is used by compiler
  public static final NotificationGroup SERVER_NOTIFICATIONS_GROUP = new NotificationGroup("CodeScan: Server Notifications", NotificationDisplayType.STICKY_BALLOON, true,
    "CodeScan");
  private static final String UPDATE_SERVER_MSG = "\n<br>Please update the binding in the CodeScan Settings";
  private static final String UPDATE_BINDING_MSG = "\n<br>Please check the CodeScan project configuration";
  private volatile boolean shown = false;
  private final Project myProject;

  protected SonarLintProjectNotifications(Project project) {
    this.myProject = project;
  }

  public static SonarLintProjectNotifications get(Project project) {
    return SonarLintUtils.getService(project, SonarLintProjectNotifications.class);
  }

  public void reset() {
    shown = false;
  }

  public void notifyConnectionIdInvalid() {
    if (shown) {
      return;
    }
    Notification notification = BINDING_PROBLEM_GROUP.createNotification(
      "<b>CodeScan - Invalid binding</b>",
      "Project bound to an invalid connection" + UPDATE_BINDING_MSG,
      NotificationType.WARNING, null);
    notification.addAction(new OpenProjectSettingsAction(myProject));
    notification.setImportant(true);
    notification.notify(myProject);
    shown = true;
  }

  public void notifyModuleInvalid() {
    if (shown) {
      return;
    }
    Notification notification = BINDING_PROBLEM_GROUP.createNotification(
      "<b>CodeScan - Invalid binding</b>",
      "Project bound to an invalid remote project" + UPDATE_BINDING_MSG,
      NotificationType.WARNING, null);
    notification.addAction(new OpenProjectSettingsAction(myProject));
    notification.setImportant(true);
    notification.notify(myProject);
    shown = true;
  }

  public void notifyModuleStale() {
    if (shown) {
      return;
    }
    Notification notification = BINDING_PROBLEM_GROUP.createNotification(
      "<b>CodeScan - Invalid binding</b>",
      "Local storage is outdated" + UPDATE_BINDING_MSG,
      NotificationType.WARNING, null);
    notification.addAction(new OpenProjectSettingsAction(myProject));
    notification.setImportant(true);
    notification.notify(myProject);
    shown = true;
  }

  public void notifyServerNeverUpdated(String serverId) {
    if (shown) {
      return;
    }
    Notification notification = BINDING_PROBLEM_GROUP.createNotification(
      "<b>CodeScan - Invalid binding</b>",
      "Missing local storage for connection '" + serverId + "'" + UPDATE_SERVER_MSG,
      NotificationType.WARNING, null);
    notification.addAction(new OpenGlobalSettingsAction(myProject));
    notification.setImportant(true);
    notification.notify(myProject);
    shown = true;
  }

  public void notifyServerStorageNeedsUpdate(String serverId) {
    if (shown) {
      return;
    }
    Notification notification = BINDING_PROBLEM_GROUP.createNotification(
      "<b>CodeScan - Invalid binding</b>",
      "Local storage for connection '" + serverId + "' must be updated" + UPDATE_SERVER_MSG,
      NotificationType.WARNING, null);
    notification.addAction(new OpenGlobalSettingsAction(myProject));
    notification.setImportant(true);
    notification.notify(myProject);
    shown = true;
  }

  public void notifyServerHasUpdates(String serverId, ConnectedSonarLintEngine engine, ServerConnection server, boolean onlyProjects) {
    Notification notification = UPDATE_GROUP.createNotification(
      "CodeScan - Binding update available",
      "Change detected for " + (server.isCodeScanCloud() ? "CodeScanCloud" : "CodeScan") + " connection '" + serverId + "'. <a href=\"#update\">Update binding now</a>",
      NotificationType.INFORMATION, new NotificationListener.Adapter() {
        @Override
        public void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
          notification.expire();
          ServerConnectionMgmtPanel.updateServerBinding(server, engine, onlyProjects);
        }
      });
    notification.notify(myProject);
  }

}
