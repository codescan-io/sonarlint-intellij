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
package org.sonarlint.intellij.ui.tree;

import com.intellij.openapi.editor.RangeMarker;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import org.sonarlint.intellij.issue.Flow;
import org.sonarlint.intellij.issue.IssueContext;
import org.sonarlint.intellij.issue.LiveIssue;
import org.sonarlint.intellij.issue.Location;
import org.sonarlint.intellij.ui.nodes.FlowNode;
import org.sonarlint.intellij.ui.nodes.FlowSecondaryLocationNode;
import org.sonarlint.intellij.ui.nodes.PrimaryLocationNode;
import org.sonarlint.intellij.ui.nodes.SummaryNode;

public class FlowsTreeModelBuilder {
  private SummaryNode summary;
  private DefaultTreeModel model;

  public DefaultTreeModel createModel() {
    summary = new SummaryNode();
    model = new DefaultTreeModel(summary);
    model.setRoot(summary);
    return model;
  }

  public void clearFlows() {
    summary = null;
    model.setRoot(null);
  }

  public void populateForIssue(LiveIssue issue) {
    RangeMarker rangeMarker = issue.getRange();
    Optional<IssueContext> context = issue.context();
    if (rangeMarker == null || !context.isPresent()) {
      clearFlows();
      return;
    }
    IssueContext issueContext = context.get();
    String message = issue.getMessage();
    if (issueContext.hasUniqueFlow()) {
      setSingleFlow(issueContext.flows().get(0), rangeMarker, message);
    } else {
      setMultipleFlows(issueContext.flows(), rangeMarker, message);
    }
  }

  private void setMultipleFlows(List<Flow> flows, RangeMarker rangeMarker, @Nullable String message) {
    summary = new SummaryNode();
    PrimaryLocationNode primaryLocationNode = new PrimaryLocationNode(rangeMarker, message, flows.get(0));
    summary.add(primaryLocationNode);

    int i = 1;
    for (Flow f : flows) {
      FlowNode flowNode = new FlowNode(f, "Flow " + i);
      primaryLocationNode.add(flowNode);

      int j = 1;
      for (Location location : f.getLocations()) {
        FlowSecondaryLocationNode locationNode = new FlowSecondaryLocationNode(j, location, f);
        flowNode.add(locationNode);
        j++;
      }
      i++;
    }
    model.setRoot(summary);
  }

  private void setSingleFlow(Flow flow, RangeMarker rangeMarker, @Nullable String message) {
    summary = new SummaryNode();
    PrimaryLocationNode primaryLocation = new PrimaryLocationNode(rangeMarker, message, flow);
    primaryLocation.setBold(true);
    summary.add(primaryLocation);

    int i = 1;
    for (Location location : flow.getLocations()) {
      FlowSecondaryLocationNode locationNode = new FlowSecondaryLocationNode(i++, location, flow);
      primaryLocation.add(locationNode);
    }

    model.setRoot(summary);
  }
}
