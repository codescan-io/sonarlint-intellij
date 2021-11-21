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
package org.sonarlint.intellij.ui;

import com.intellij.openapi.vfs.VirtualFile;
import java.util.Comparator;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.intellij.ui.nodes.AbstractNode;
import org.sonarlint.intellij.ui.nodes.FileNode;
import org.sonarlint.intellij.ui.nodes.SummaryNode;
import org.sonarlint.intellij.ui.tree.TreeCellRenderer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractNodeTest {
  private AbstractNode testNode;

  @Before
  public void setUp() {
    testNode = new AbstractNode() {
      @Override public void render(TreeCellRenderer renderer) {
        // do nothing
      }
    };
  }

  @Test
  public void testInsertion() {
    SummaryNode summaryNode = new SummaryNode();
    assertThat(summaryNode.insertFileNode(new FileNode(mockFile("name")), nameComparator)).isZero();
    assertThat(summaryNode.insertFileNode(new FileNode(mockFile("file")), nameComparator)).isZero();
    assertThat(summaryNode.insertFileNode(new FileNode(mockFile("test")), nameComparator)).isEqualTo(2);
    assertThat(summaryNode.insertFileNode(new FileNode(mockFile("abc")), nameComparator)).isZero();

    assertThat(summaryNode.getChildCount()).isEqualTo(4);
    assertThat(((FileNode) summaryNode.getChildAt(0)).file().getName()).isEqualTo("abc");
    assertThat(((FileNode) summaryNode.getChildAt(1)).file().getName()).isEqualTo("file");
    assertThat(((FileNode) summaryNode.getChildAt(2)).file().getName()).isEqualTo("name");
    assertThat(((FileNode) summaryNode.getChildAt(3)).file().getName()).isEqualTo("test");
  }

  @Test
  public void testFileCount() {
    AbstractNode child1 = mock(AbstractNode.class);
    AbstractNode child2 = mock(AbstractNode.class);
    AbstractNode child3 = mock(AbstractNode.class);

    when(child1.getIssueCount()).thenReturn(1);

    when(child2.getIssueCount()).thenReturn(2);

    when(child3.getIssueCount()).thenReturn(3);

    testNode.add(child1);
    testNode.add(child2);
    testNode.add(child3);

    assertThat(testNode.getIssueCount()).isEqualTo(6);
    assertThat(testNode.getIssueCount()).isEqualTo(6);

    //second call should be from cache
    verify(child1, times(1)).getIssueCount();
  }

  private final Comparator<FileNode> nameComparator = Comparator.comparing(f -> f.file().getName());

  private VirtualFile mockFile(String name) {
    VirtualFile file = mock(VirtualFile.class);
    when(file.getName()).thenReturn(name);
    return file;
  }

}
