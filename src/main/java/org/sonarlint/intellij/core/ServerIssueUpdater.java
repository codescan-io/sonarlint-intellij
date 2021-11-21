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
package org.sonarlint.intellij.core;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonarlint.intellij.common.ui.SonarLintConsole;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.config.global.ServerConnection;
import org.sonarlint.intellij.config.project.SonarLintProjectSettings;
import org.sonarlint.intellij.exception.InvalidBindingException;
import org.sonarlint.intellij.issue.IssueManager;
import org.sonarlint.intellij.issue.ServerIssueTrackable;
import org.sonarlint.intellij.issue.tracking.Trackable;
import org.sonarlint.intellij.util.SonarLintAppUtils;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBinding;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.exceptions.DownloadException;

import static org.sonarlint.intellij.common.util.SonarLintUtils.getService;
import static org.sonarlint.intellij.config.Settings.getSettingsFor;

public class ServerIssueUpdater implements Disposable {

  private static final int THREADS_NUM = 5;
  private static final int QUEUE_LIMIT = 100;
  private static final int FETCH_ALL_ISSUES_THRESHOLD = 10;
  private final Project myProject;

  private final ExecutorService executorService;

  public ServerIssueUpdater(Project project) {
    myProject = project;

    // Equivalent to Executors.newFixedThreadPool(THREADS_NUM), but instead of the default unlimited LinkedBlockingQueue,
    // we use ArrayBlockingQueue with a cap. This means that if QUEUE_LIMIT tasks are already queued (and THREADS_NUM being executed),
    // new tasks will be rejected with RejectedExecutionException.
    // http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_LIMIT);
    this.executorService = new ThreadPoolExecutor(THREADS_NUM, THREADS_NUM, 0L, TimeUnit.MILLISECONDS, queue);
  }

  public void fetchAndMatchServerIssues(Map<Module, Collection<VirtualFile>> filesPerModule, ProgressIndicator indicator, boolean waitForCompletion) {
    SonarLintProjectSettings projectSettings = getSettingsFor(myProject);
    if (!projectSettings.isBound()) {
      // not in connected mode
      return;
    }
    try {
      ProjectBindingManager projectBindingManager = getService(myProject, ProjectBindingManager.class);
      ServerConnection connection = projectBindingManager.getServerConnection();
      ConnectedSonarLintEngine engine = projectBindingManager.getConnectedEngine();

      int numFiles = filesPerModule.values().stream().mapToInt(Collection::size).sum();
      boolean downloadAll = numFiles >= FETCH_ALL_ISSUES_THRESHOLD;
      String msg;

      if (downloadAll) {
        msg = "Fetching all server issues";
      } else {
        msg = "Fetching server issues in " + numFiles + SonarLintUtils.pluralize(" file", numFiles);
      }
      if (waitForCompletion) {
        msg += " (waiting for results)";
      }
      SonarLintConsole console = getService(myProject, SonarLintConsole.class);
      console.debug(msg);
      indicator.setText(msg);

      // submit tasks
      List<Future<Void>> updateTasks = fetchAndMatchServerIssues(filesPerModule, connection, engine, downloadAll);
      if (waitForCompletion) {
        waitForTasks(updateTasks);
      }
    } catch (InvalidBindingException e) {
      // ignore, do nothing
    }
  }

  private void waitForTasks(List<Future<Void>> updateTasks) {
    for (Future<Void> f : updateTasks) {
      try {
        f.get(20, TimeUnit.SECONDS);
      } catch (TimeoutException ex) {
        f.cancel(true);
        SonarLintConsole.get(myProject).error("ServerIssueUpdater task expired", ex);
      } catch (Exception ex) {
        SonarLintConsole.get(myProject).error("ServerIssueUpdater task failed", ex);
      }
    }
  }

  private List<Future<Void>> fetchAndMatchServerIssues(Map<Module, Collection<VirtualFile>> filesPerModule,
    ServerConnection server, ConnectedSonarLintEngine engine, boolean downloadAll) {
    List<Future<Void>> futureList = new LinkedList<>();

    if (!downloadAll) {
      for (Map.Entry<Module, Collection<VirtualFile>> e : filesPerModule.entrySet()) {
        String projectKey = getService(e.getKey(), ModuleBindingManager.class).resolveProjectKey();
        futureList.addAll(fetchAndMatchServerIssues(Objects.requireNonNull(projectKey), e.getKey(), e.getValue(), server, engine));
      }
    } else {
      futureList.addAll(downloadAndMatchAllServerIssues(filesPerModule, server, engine));
    }
    return futureList;
  }

  private List<Future<Void>> downloadAndMatchAllServerIssues(Map<Module, Collection<VirtualFile>> filesPerModule, ServerConnection server,
    ConnectedSonarLintEngine engine) {
    IssueUpdater issueUpdater = new IssueUpdater(server, engine);
    List<Future<Void>> futuresList = new ArrayList<>();
    Set<String> updatedProjects = ConcurrentHashMap.newKeySet();
    for (Map.Entry<Module, Collection<VirtualFile>> e : filesPerModule.entrySet()) {
      String projectKey = getService(e.getKey(), ModuleBindingManager.class).resolveProjectKey();
      Runnable task = () -> {
        if (updatedProjects.add(projectKey)) {
          issueUpdater.downloadAllServerIssues(Objects.requireNonNull(projectKey));
        }
        ProjectBinding binding = getProjectBinding(e.getKey());
        Map<VirtualFile, String> relativePathPerFile = getRelativePaths(e.getKey().getProject(), e.getValue());

        for (Map.Entry<VirtualFile, String> entry : relativePathPerFile.entrySet()) {
          issueUpdater.fetchAndMatchFile(binding, entry.getKey(), entry.getValue());
        }
      };
      futuresList.add(submit(task, Objects.requireNonNull(projectKey), null));
    }

    return futuresList;
  }

  private ProjectBinding getProjectBinding(Module module) {
    ModuleBindingManager moduleBindingManager = getService(module, ModuleBindingManager.class);
    return moduleBindingManager.getBinding();
  }

  private List<Future<Void>> fetchAndMatchServerIssues(String projectKey, Module module, Collection<VirtualFile> files, ServerConnection server, ConnectedSonarLintEngine engine) {
    List<Future<Void>> futureList = new LinkedList<>();
    ProjectBinding binding = getProjectBinding(module);
    Map<VirtualFile, String> relativePathPerFile = getRelativePaths(module.getProject(), files);
    IssueUpdater issueUpdater = new IssueUpdater(server, engine);

    for (Map.Entry<VirtualFile, String> e : relativePathPerFile.entrySet()) {
      Runnable task = () -> issueUpdater.downloadAndMatchFile(binding, e.getKey(), e.getValue());
      futureList.add(submit(task, projectKey, e.getValue()));
    }
    return futureList;
  }

  private static Map<VirtualFile, String> getRelativePaths(Project project, Collection<VirtualFile> files) {
    return ApplicationManager.getApplication().<Map<VirtualFile, String>>runReadAction(() -> {
      Map<VirtualFile, String> relativePathPerFile = new HashMap<>();

      for (VirtualFile file : files) {
        String relativePath = SonarLintAppUtils.getRelativePathForAnalysis(project, file);
        if (relativePath != null) {
          relativePathPerFile.put(file, relativePath);
        }
      }
      return relativePathPerFile;
    });
  }

  private Future<Void> submit(Runnable task, String projectKey, @Nullable String moduleRelativePath) {
    try {
      return this.executorService.submit(task, null);
    } catch (RejectedExecutionException e) {
      SonarLintConsole.get(myProject).error("fetch and match server issues rejected for projectKey=" + projectKey + ", filepath=" + moduleRelativePath, e);
      return CompletableFuture.completedFuture(null);
    }
  }

  @Override
  public void dispose() {
    List<Runnable> rejected = executorService.shutdownNow();
    if (!rejected.isEmpty()) {
      SonarLintConsole.get(myProject).error("rejected " + rejected.size() + " pending tasks");
    }
  }

  private class IssueUpdater {
    private final ServerConnection server;
    private final ConnectedSonarLintEngine engine;

    private IssueUpdater(ServerConnection server, ConnectedSonarLintEngine engine) {
      this.server = server;
      this.engine = engine;
    }

    public void fetchAndMatchFile(ProjectBinding projectBinding, VirtualFile virtualFile, String relativePath) {
      List<ServerIssue> serverIssues = engine.getServerIssues(projectBinding, relativePath);
      matchFile(virtualFile, serverIssues);
    }

    public void downloadAndMatchFile(ProjectBinding projectBinding, VirtualFile virtualFile, String relativePath) {
      List<ServerIssue> serverIssues = fetchServerIssuesForFile(projectBinding, relativePath);
      matchFile(virtualFile, serverIssues);
    }

    public void downloadAllServerIssues(String projectKey) {
      try {
        SonarLintConsole.get(myProject).debug("fetchServerIssues projectKey=" + projectKey);
        engine.downloadServerIssues(server.getEndpointParams(), server.getHttpClient(), projectKey, true, null);
      } catch (DownloadException e) {
        SonarLintConsole console = getService(myProject, SonarLintConsole.class);
        console.info(e.getMessage());
      }
    }

    private void matchFile(VirtualFile virtualFile, List<ServerIssue> serverIssues) {
      try {
        Collection<Trackable> serverIssuesTrackable = serverIssues.stream()
          .map(ServerIssueTrackable::new)
          .collect(Collectors.toList());

        if (!serverIssuesTrackable.isEmpty()) {
          IssueManager issueManager = getService(myProject, IssueManager.class);
          issueManager.matchWithServerIssues(virtualFile, serverIssuesTrackable);
        }
      } catch (Throwable t) {
        // note: without catching Throwable, any exceptions raised in the thread will not be visible
        SonarLintConsole console = getService(myProject, SonarLintConsole.class);
        console.error("error while fetching and matching server issues", t);
      }
    }

    private List<ServerIssue> fetchServerIssuesForFile(ProjectBinding projectBinding, String relativePath) {
      try {
        SonarLintConsole.get(myProject).debug("fetchServerIssues projectKey=" + projectBinding.projectKey() + ", filepath=" + relativePath);
        return engine.downloadServerIssues(server.getEndpointParams(), server.getHttpClient(), projectBinding, relativePath, true, null);
      } catch (DownloadException e) {
        SonarLintConsole console = getService(myProject, SonarLintConsole.class);
        console.info(e.getMessage());
        return engine.getServerIssues(projectBinding, relativePath);
      }
    }
  }
}
