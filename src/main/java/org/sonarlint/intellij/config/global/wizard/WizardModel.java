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
package org.sonarlint.intellij.config.global.wizard;

import com.intellij.openapi.progress.ProgressManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.config.global.ServerConnection;
import org.sonarlint.intellij.tasks.CheckNotificationsSupportedTask;
import org.sonarlint.intellij.tasks.GetOrganizationTask;
import org.sonarlint.intellij.tasks.GetOrganizationsTask;
import org.sonarsource.sonarlint.core.serverapi.organization.ServerOrganization;

public class WizardModel {
  private static final String SONARCLOUD_URL = "https://app.codescan.io";
  private ServerType serverType;
  private String serverUrl;
  private String token;
  private String login;
  private char[] password;
  private String name;
  private String organizationKey;
  private boolean proxyEnabled;
  private boolean notificationsDisabled = false;
  private boolean notificationsSupported = false;

  private List<ServerOrganization> organizationList = new ArrayList<>();

  public enum ServerType {
    SONARCLOUD,
    SONARQUBE
  }

  public WizardModel() {

  }

  public WizardModel(ServerConnection connectionToEdit) {
    if (SonarLintUtils.isCodeScanCloudAlias(connectionToEdit.getHostUrl())) {
      serverType = ServerType.SONARCLOUD;
    } else {
      serverType = ServerType.SONARQUBE;
      serverUrl = connectionToEdit.getHostUrl();
    }
    this.proxyEnabled = connectionToEdit.enableProxy();
    this.token = connectionToEdit.getToken();
    this.login = connectionToEdit.getLogin();
    String pass = connectionToEdit.getPassword();
    if (pass != null) {
      this.password = pass.toCharArray();
    }
    this.organizationKey = connectionToEdit.getOrganizationKey();
    this.notificationsDisabled = connectionToEdit.isDisableNotifications();
    this.name = connectionToEdit.getName();
  }

  @CheckForNull
  public ServerType getServerType() {
    return serverType;
  }

  public WizardModel setServerType(ServerType serverType) {
    this.serverType = serverType;
    return this;
  }

  public boolean isNotificationsSupported() {
    return notificationsSupported;
  }

  public WizardModel setNotificationsSupported(boolean notificationsSupported) {
    this.notificationsSupported = notificationsSupported;
    return this;
  }

  public void queryIfNotificationsSupported() throws Exception {
    final ServerConnection partialConnection = createConnectionWithoutOrganization();
    if (partialConnection.isSonarCloud()) {
      setNotificationsSupported(true);
    } else {
      CheckNotificationsSupportedTask task = new CheckNotificationsSupportedTask(partialConnection);
      ProgressManager.getInstance().run(task);
      if (task.getException() != null) {
        throw task.getException();
      }
      setNotificationsSupported(task.notificationsSupported());
    }
  }

  public void queryOrganizations() throws Exception {
    final ServerConnection partialConnection = createConnectionWithoutOrganization();
    if (partialConnection.isSonarCloud()) {
      GetOrganizationsTask task = new GetOrganizationsTask(partialConnection);
      ProgressManager.getInstance().run(task);
      if (task.getException() != null) {
        throw task.getException();
      }
      setOrganizationList(task.organizations());
      final String presetOrganizationKey = getOrganizationKey();
      if (presetOrganizationKey != null) {
        // the previously configured organization might not be in the list. If that's the case, fetch it and add it to the list.
        boolean orgExists = task.organizations().stream().anyMatch(o -> o.getKey().equals(presetOrganizationKey));
        if (!orgExists) {
          GetOrganizationTask getOrganizationTask = new GetOrganizationTask(partialConnection, presetOrganizationKey);
          ProgressManager.getInstance().run(getOrganizationTask);
          final Optional<ServerOrganization> fetchedOrganization = getOrganizationTask.organization();
          if (getOrganizationTask.getException() != null || !fetchedOrganization.isPresent()) {
            // ignore and reset organization
            setOrganizationKey(null);
          } else {
            getOrganizationList().add(fetchedOrganization.get());
          }
        }
      }
      if (getOrganizationKey() == null && task.organizations().size() == 1) {
        // if there is only one organization, we can preselect it
        setOrganizationKey(task.organizations().iterator().next().getKey());
      }
    }
  }

  public boolean isNotificationsDisabled() {
    return notificationsDisabled;
  }

  public WizardModel setNotificationsDisabled(boolean notificationsDisabled) {
    this.notificationsDisabled = notificationsDisabled;
    return this;
  }

  public boolean isProxyEnabled() {
    return proxyEnabled;
  }

  public WizardModel setProxyEnabled(boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
    return this;
  }

  public List<ServerOrganization> getOrganizationList() {
    return organizationList;
  }

  public WizardModel setOrganizationList(List<ServerOrganization> organizationList) {
    this.organizationList = organizationList;
    return this;
  }

  @CheckForNull
  public String getServerUrl() {
    return serverUrl;
  }

  public WizardModel setServerUrl(@Nullable String serverUrl) {
    this.serverUrl = serverUrl;
    return this;
  }

  @CheckForNull
  public String getToken() {
    return token;
  }

  public WizardModel setToken(@Nullable String token) {
    this.token = token;
    return this;
  }

  @CheckForNull
  public String getLogin() {
    return login;
  }

  public WizardModel setLogin(@Nullable String login) {
    this.login = login;
    return this;
  }

  @CheckForNull
  public char[] getPassword() {
    return password;
  }

  public WizardModel setPassword(@Nullable char[] password) {
    this.password = password;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public WizardModel setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getOrganizationKey() {
    return organizationKey;
  }

  public WizardModel setOrganizationKey(@Nullable String organization) {
    this.organizationKey = organization;
    return this;
  }

  public ServerConnection createConnectionWithoutOrganization() {
    return createConnection(null);
  }

  public ServerConnection createConnection() {
    return createConnection(organizationKey);
  }

  private ServerConnection createConnection(@Nullable String organizationKey) {
    ServerConnection.Builder builder = ServerConnection.newBuilder()
      .setOrganizationKey(organizationKey)
      .setEnableProxy(proxyEnabled)
      .setName(name);

    if (serverType == ServerType.SONARCLOUD) {
      builder.setHostUrl(SONARCLOUD_URL);

    } else {
      builder.setHostUrl(serverUrl);
    }

    if (token != null) {
      builder.setToken(token)
        .setLogin(null)
        .setPassword(null);
    } else {
      builder.setToken(null)
        .setLogin(login)
        .setPassword(new String(password));
    }
    builder.setDisableNotifications(notificationsDisabled);
    return builder.build();
  }
}
