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
package org.sonarlint.intellij.trigger;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.UIUtil;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.actions.SonarLintToolWindow;
import org.sonarlint.intellij.analysis.AnalysisCallback;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.issue.IssueManager;
import org.sonarlint.intellij.issue.IssueStore;
import org.sonarlint.intellij.issue.LiveIssue;
import org.sonarsource.sonarlint.core.client.api.common.Language;

import static org.sonarlint.intellij.config.Settings.getGlobalSettings;

public class SonarLintCheckinHandler extends CheckinHandler {
  private static final Logger LOGGER = Logger.getInstance(SonarLintCheckinHandler.class);
  private static final String ACTIVATED_OPTION_NAME = "SONARLINT_PRECOMMIT_ANALYSIS";

  private final Project project;
  private final CheckinProjectPanel checkinPanel;
  private JCheckBox checkBox;

  public SonarLintCheckinHandler(Project project, CheckinProjectPanel checkinPanel) {
    this.project = project;
    this.checkinPanel = checkinPanel;
  }

  @Override
  @Nullable
  public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
    this.checkBox = new NonFocusableCheckBox("Perform CodeScan analysis");
    return new MyRefreshableOnComponent(checkBox);
  }

  @Override
  public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
    if (checkBox != null && !checkBox.isSelected()) {
      return ReturnResult.COMMIT;
    }

    // de-duplicate as the same file can be present several times in the panel (e.g. in several changelists)
    Set<VirtualFile> affectedFiles = new HashSet<>(checkinPanel.getVirtualFiles());
    SonarLintSubmitter submitter = SonarLintUtils.getService(project, SonarLintSubmitter.class);
    // this will block EDT (modal)
    try {
      AtomicBoolean error = new AtomicBoolean(false);
      AnalysisCallback callback = new AnalysisCallback() {
        @Override public void onSuccess(Set<VirtualFile> failedVirtualFiles) {
          // do nothing
        }

        @Override public void onError(Throwable e) {
          error.set(true);
        }
      };
      submitter.submitFilesModal(affectedFiles, TriggerType.CHECK_IN, callback);
      if (error.get()) {
        return ReturnResult.CANCEL;
      }
      return processResult(affectedFiles);
    } catch (Exception e) {
      handleError(e, affectedFiles.size());
      return ReturnResult.CANCEL;
    }
  }

  private void handleError(Exception e, int numFiles) {
    String msg = "CodeScan - Error analysing " + numFiles + " changed file(s).";
    if (e.getMessage() != null) {
      msg = msg + ": " + e.getMessage();
    }
    LOGGER.info(msg, e);
    Messages.showErrorDialog(project, msg, "Error Analysing Files");
  }

  private ReturnResult processResult(Set<VirtualFile> affectedFiles) {
    IssueStore issueStore = SonarLintUtils.getService(project, IssueStore.class);
    IssueManager issueManager = SonarLintUtils.getService(project, IssueManager.class);

    Map<VirtualFile, Collection<LiveIssue>> map = affectedFiles.stream()
      .collect(Collectors.toMap(Function.identity(), issueManager::getForFile));

    long numIssues = map.entrySet().stream()
      .flatMap(e -> e.getValue().stream())
      .filter(i -> !i.isResolved())
      .count();
    issueStore.set(map, "SCM changed files");

    long numBlockerIssues = map.entrySet().stream()
      .flatMap(e -> e.getValue().stream())
      .filter(i -> !i.isResolved())
      .filter(i -> "BLOCKER".equals(i.getSeverity()))
      .count();

    if (numIssues == 0) {
      return ReturnResult.COMMIT;
    }

    long numFiles = map.keySet().size();

    List<LiveIssue> issues = map.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    long numSecretsIssues = issues.stream().filter(issue -> issue.getRuleKey().startsWith(Language.SECRETS.getPluginKey())).count();
    String msg = createMessage(numFiles, numIssues, numBlockerIssues, numSecretsIssues);

    return showYesNoCancel(msg);
  }

  private static String createMessage(long filesAnalyzed, long numIssues, long numBlockerIssues, long numSecretsIssues) {
    String files = filesAnalyzed == 1 ? "file" : "files";
    String issues = numIssues == 1 ? "issue" : "issues";

    String warningAboutLeakedSecrets = "";
    if (numSecretsIssues > 0) {
      String secretWord = numSecretsIssues == 1 ? "secret" : "secrets";
      warningAboutLeakedSecrets = String.format("\n\nCodeScan analysis found %d %s. " +
        "Committed secrets may lead to unauthorized system access.", numSecretsIssues, secretWord);
    }
    StringBuilder message = new StringBuilder();
    if (numBlockerIssues > 0) {
      String blocker = numBlockerIssues == 1 ? "issue" : "issues";
      message.append(String.format("CodeScan analysis on %d %s found %d %s (including %d blocker %s)", filesAnalyzed, files,
        numIssues, issues, numBlockerIssues, blocker));
    } else {
      message.append(String.format("CodeScan analysis on %d %s found %d %s", filesAnalyzed, files, numIssues, issues));
    }
    message.append(warningAboutLeakedSecrets);
    return message.toString();
  }

  private ReturnResult showYesNoCancel(String resultStr) {
    final int answer = Messages.showYesNoCancelDialog(project,
      resultStr,
      "CodeScan Analysis Results",
      "&Review Issues",
      "Comm&it Anyway",
      "Close",
      UIUtil.getWarningIcon());

    if (answer == Messages.YES) {
      showChangedFilesTab();
      return ReturnResult.CLOSE_WINDOW;
    } else if (answer == Messages.CANCEL) {
      return ReturnResult.CANCEL;
    } else {
      return ReturnResult.COMMIT;
    }
  }

  private void showChangedFilesTab() {
    SonarLintUtils.getService(project, SonarLintToolWindow.class).openAnalysisResults();
  }

  private class MyRefreshableOnComponent implements RefreshableOnComponent {
    private final JCheckBox checkBox;

    private MyRefreshableOnComponent(JCheckBox checkBox) {
      this.checkBox = checkBox;
    }

    @Override
    public JComponent getComponent() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(checkBox);
      boolean dumb = DumbService.isDumb(project);
      checkBox.setEnabled(!dumb);
      checkBox.setToolTipText(dumb ? "CodeScan analysis is impossible until indices are up-to-date" : "");
      return panel;
    }

    @Override
    public void refresh() {
      // nothing to do
    }

    @Override
    public void saveState() {
      PropertiesComponent.getInstance(project).setValue(ACTIVATED_OPTION_NAME, Boolean.toString(checkBox.isSelected()));
    }

    @Override
    public void restoreState() {
      PropertiesComponent props = PropertiesComponent.getInstance(project);
      checkBox.setSelected(props.getBoolean(ACTIVATED_OPTION_NAME, getGlobalSettings().isAutoTrigger()));
    }
  }
}
