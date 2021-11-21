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
package org.sonarlint.intellij.ui.nodes;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;

import java.util.Objects;
import javax.swing.Icon;

import org.sonarlint.intellij.ui.tree.TreeCellRenderer;

import static org.sonarlint.intellij.common.util.SonarLintUtils.pluralize;

public class FileNode extends AbstractNode {
  private final VirtualFile file;

  public FileNode(VirtualFile file) {
    this.file = file;
  }

  public VirtualFile file() {
    return file;
  }

  @Override
  public int getIssueCount() {
    return super.getChildCount();
  }

  public Icon getIcon() {
    return file.getFileType().getIcon();
  }

  @Override
  public void render(TreeCellRenderer renderer) {
    renderer.setIcon(getIcon());
    renderer.append(file.getName());
    renderer.append(spaceAndThinSpace() + "(" + getIssueCount() + pluralize(" issue", getIssueCount()) + ")", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileNode fileNode = (FileNode) o;
    return Objects.equals(file, fileNode.file);
  }

  @Override
  public int hashCode() {
    return Objects.hash(file);
  }
}
