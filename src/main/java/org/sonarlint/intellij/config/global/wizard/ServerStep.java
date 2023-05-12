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

import com.intellij.ide.wizard.AbstractWizardStepEx;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.ui.SwingHelper;
import icons.SonarLintIcons;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.common.util.SonarLintUtils;

public class ServerStep extends AbstractWizardStepEx {
  private static final int NAME_MAX_LENGTH = 50;
  private final WizardModel model;
  private final Collection<String> existingNames;

  private JRadioButton radioCodeScanCloud;
  private JRadioButton radioCodeScan;
  private JPanel panel;
  private JTextField urlText;
  private JLabel urlLabel;
  private JTextField nameField;
  private JLabel sonarcloudIcon;
  private JLabel sonarqubeIcon;
  private JEditorPane sonarcloudText;
  private JEditorPane sonarqubeText;
  private JButton proxyButton;
  private ErrorPainter errorPainter;

  public ServerStep(WizardModel model, boolean editing, Collection<String> existingNames) {
    super("Server Details");
    this.model = model;
    this.existingNames = existingNames;
    radioCodeScanCloud.addChangeListener(e -> selectionChanged());
    radioCodeScan.addChangeListener(e -> selectionChanged());

    DocumentListener listener = new DocumentAdapter() {
      @Override protected void textChanged(DocumentEvent e) {
        fireStateChanged();
      }
    };
    urlText.getDocument().addDocumentListener(listener);
    nameField.getDocument().addDocumentListener(listener);

    nameField.setToolTipText("Name of this configuration (mandatory field)");

    String cloudText = "To connect to <a href=\"" + CodescanCloudConstants.CODESCAN_US_URL + "\">"
            + CodescanCloudConstants.CODESCAN_US_URL + "</a>";
    sonarcloudText.setText(cloudText);
    sonarcloudText.addHyperlinkListener(new BrowserHyperlinkListener());

    String sqText = "Connect to other CodeScan instances or a server";
    sonarqubeText.setText(sqText);

    if (!editing) {
      sonarqubeIcon.addMouseListener(new MouseInputAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          radioCodeScan.setSelected(true);
        }
      });
      sonarcloudIcon.addMouseListener(new MouseInputAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          radioCodeScanCloud.setSelected(true);
        }
      });
    }

    proxyButton.addActionListener(evt -> {
      HttpConfigurable.editConfigurable(panel);
    });

    load(editing);
    paintErrors();
  }

  private void paintErrors() {
    errorPainter = new ErrorPainter();
    errorPainter.installOn(panel, this);
  }

  private void load(boolean editing) {
    Icon sqIcon = SonarLintIcons.ICON_CODESCAN;
    Icon clIcon = SonarLintIcons.ICON_CODESCAN;

    if ((model.getServerType() == WizardModel.ServerType.SONARCLOUD &&
            CodescanCloudConstants.CODESCAN_US_URL.equals(model.getServerUrl())) || model.getServerType() == null) {
      radioCodeScanCloud.setSelected(true);
      if (editing) {
        sqIcon = SonarLintIcons.toDisabled(sqIcon);
      }
    } else {
      radioCodeScan.setSelected(true);
      urlText.setText(model.getServerUrl());
      if (editing) {
        clIcon = SonarLintIcons.toDisabled(clIcon);
      }
    }

    nameField.setText(model.getName());

    if (editing) {
      nameField.setEnabled(false);
      radioCodeScan.setEnabled(false);
      radioCodeScanCloud.setEnabled(false);
    }

    sonarqubeIcon.setIcon(sqIcon);
    sonarcloudIcon.setIcon(clIcon);
  }

  private void selectionChanged() {
    boolean sq = radioCodeScan.isSelected();

    urlText.setEnabled(sq);
    urlLabel.setEnabled(sq);
    sonarqubeText.setEnabled(sq);
    sonarcloudText.setEnabled(!sq);
    fireStateChanged();
  }

  @Override
  public JComponent getComponent() {
    return panel;
  }

  @NotNull @Override public Object getStepId() {
    return ServerStep.class;
  }

  @Nullable @Override public Object getNextStepId() {
    return AuthStep.class;
  }

  @Nullable @Override public Object getPreviousStepId() {
    return null;
  }

  @Override public boolean isComplete() {
    boolean nameValid = !nameField.getText().trim().isEmpty();
    errorPainter.setValid(nameField, nameValid);
    boolean urlValid = radioCodeScanCloud.isSelected() || !urlText.getText().trim().isEmpty();
    errorPainter.setValid(urlText, urlValid);

    return nameValid && urlValid;
  }

  @Override public void commit(CommitType commitType) throws CommitStepException {
    validateName();
    validateUrl();
    save();
  }

  private void validateName() throws CommitStepException {
    if (existingNames.contains(nameField.getText().trim())) {
      throw new CommitStepException("There is already a configuration with that name. Please choose another name");
    }
  }

  private void validateUrl() throws CommitStepException {
    if (radioCodeScan.isSelected()) {
      try {
        URL url = new URL(urlText.getText());
        if (SonarLintUtils.isBlank(url.getHost())) {
          throw new CommitStepException("Please provide a valid URL");
        }
      } catch (MalformedURLException e) {
        throw new CommitStepException("Please provide a valid URL");
      }
    }
  }

  private void save() {
    if (radioCodeScanCloud.isSelected()) {
      model.setServerType(WizardModel.ServerType.SONARCLOUD);
      model.setServerUrl(CodescanCloudConstants.CODESCAN_US_URL);
    } else {
      String serverUrl = urlText.getText().trim();
      serverUrl = StringUtils.removeEnd(serverUrl, "/");
      model.setServerUrl(serverUrl);
      if (SonarLintUtils.isCodeScanCloudAlias(serverUrl)) {
        model.setServerType(WizardModel.ServerType.SONARCLOUD);
      } else {
        model.setServerType(WizardModel.ServerType.SONARQUBE);
      }
    }
    model.setName(nameField.getText().trim());
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    if (nameField.isEnabled()) {
      return nameField;
    } else if (urlText.isEnabled()) {
      return urlText;
    }
    return null;
  }

  private void createUIComponents() {
    sonarcloudIcon = new JLabel(SonarLintIcons.ICON_CODESCAN);
    sonarqubeIcon = new JLabel(SonarLintIcons.ICON_CODESCAN);
    sonarcloudText = SwingHelper.createHtmlViewer(false, null, null, null);
    sonarqubeText = SwingHelper.createHtmlViewer(false, null, null, null);

    JBTextField text = new JBTextField();
    text.getEmptyText().setText("Example: http://localhost:9000");
    urlText = text;

    nameField = new JBTextField();
    nameField.setDocument(new LengthRestrictedDocument(NAME_MAX_LENGTH));
  }
}
