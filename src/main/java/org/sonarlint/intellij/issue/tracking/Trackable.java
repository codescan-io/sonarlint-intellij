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
package org.sonarlint.intellij.issue.tracking;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public interface Trackable {

  /**
   * The line index, starting with 1. Null means that
   * issue does not relate to a line (file issue for example).
   */
  @CheckForNull
  Integer getLine();

  String getMessage();

  @CheckForNull
  Integer getTextRangeHash();

  @CheckForNull
  Integer getLineHash();

  String getRuleKey();

  @CheckForNull
  String getServerIssueKey();

  @CheckForNull
  Long getCreationDate();

  boolean isResolved();

  String getAssignee();

  String getSeverity();

  @Nullable
  String getType();
}
