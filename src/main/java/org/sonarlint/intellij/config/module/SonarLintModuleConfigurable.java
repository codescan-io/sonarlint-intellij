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
package org.sonarlint.intellij.config.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class SonarLintModuleConfigurable implements Configurable, Configurable.NoMargin, Configurable.NoScroll {
  private final Module module;
  private SonarLintModulePanel panel;

  public SonarLintModuleConfigurable(Module module) {
    this.module = module;
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  @Override
  public String getDisplayName() {
    return "CodeScan";
  }

  @Nullable @Override
  public JComponent createComponent() {
    if (panel == null) {
      panel = new SonarLintModulePanel(module);
    }
    return panel.getRootPanel();
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void reset() {
    if (panel != null) {
      panel.load();
    }
  }

  @javax.annotation.Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public void apply() {
    // this configurable is read-only, nothing to save
  }

  @Override
  public void disposeUIResources() {
    // nothing to do
  }
}
