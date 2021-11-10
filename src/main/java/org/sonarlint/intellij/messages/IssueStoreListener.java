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
package org.sonarlint.intellij.messages;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;

/**
 * Called when issue store is updated. Should be used by UI that displays issues.
 */
public interface IssueStoreListener {
  Topic<IssueStoreListener> SONARLINT_ISSUE_STORE_TOPIC = Topic.create("Issue store changed", IssueStoreListener.class);

  void filesChanged(Set<VirtualFile> changedFiles);

  default void fileChanged(VirtualFile virtualFile) {
    filesChanged(new HashSet<>(singletonList(virtualFile)));
  }

  void allChanged();
}
