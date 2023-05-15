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
package org.sonarlint.intellij.errorsubmitter;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus;
import com.intellij.util.Consumer;
import java.awt.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.SonarLintPlugin;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.config.global.wizard.CodescanCloudConstants;
import org.sonarlint.intellij.http.ApacheHttpClient;
import org.sonarsource.sonarlint.core.serverapi.HttpClient;

// Inspired from https://github.com/openclover/clover/blob/master/clover-idea/src/com/atlassian/clover/idea/util/BlameClover.java
public class BlameSonarSource extends ErrorReportSubmitter {

  private static final Map<String, String> packageAbbreviation;

  static {
    Map<String, String> aMap = new LinkedHashMap<>();
    aMap.put("com.intellij.openapi.", "c.ij.oa.");
    aMap.put("com.intellij.", "c.ij.");
    aMap.put("org.sonarlint.intellij.", "o.sl.ij.");
    aMap.put("org.sonarsource.sonarlint.", "o.ss.sl.");
    aMap.put("org.sonar.plugins.", "o.s.pl.");
    packageAbbreviation = Collections.unmodifiableMap(aMap);
  }

  @Override
  public String getReportActionText() {
    return "Report to CodeScan Support";
  }

  @Override
  public boolean submit(@NotNull IdeaLoggingEvent[] events,
    @Nullable String additionalInfo,
    @NotNull Component parentComponent,
    @NotNull Consumer<SubmittedReportInfo> consumer) {
    String body = buildBody(events, additionalInfo);
    HttpClient.Response response = ApacheHttpClient.getDefault()
            .post(CodescanCloudConstants.CODESCAN_ERROR_ENDPOINT, "text/html", body);
    SubmissionStatus status = response.isSuccessful() ? SubmissionStatus.NEW_ISSUE : SubmissionStatus.FAILED;
    consumer.consume(new SubmittedReportInfo(status));
    return response.isSuccessful();
  }

  @NotNull
  static String buildBody(@NotNull IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo) {
    StringBuilder body = new StringBuilder();
    body.append("<h4>Environment:</h4>");
    body.append("<ul style=\"list-style-type:disc;\">");
    body.append("<li> Java: ").append(System.getProperty("java.vendor")).append(" ").append(System.getProperty("java.version")).append("</li>");
    body.append("<li> OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.arch")).append("</li>");
    body.append("<li> IDE: ").append(ApplicationInfo.getInstance().getFullApplicationName()).append("</li>");
    body.append("<li> CodeScan: ").append(SonarLintUtils.getService(SonarLintPlugin.class).getVersion()).append("</li>");
    body.append("</ul>");
    if (additionalInfo != null) {
      body.append(additionalInfo);
      body.append("<br/>\n");
    }
    for (IdeaLoggingEvent ideaLoggingEvent : events) {
      final String message = ideaLoggingEvent.getMessage();
      if (StringUtils.isNotBlank(message)) {
        body.append(message).append("<br/>\n");
      }
      final String throwableText = ideaLoggingEvent.getThrowableText();
      if (StringUtils.isNotBlank(throwableText)) {
        body.append("<br/>\n");
        body.append(abbreviate(throwableText));
        body.append("<br/>\n");
      }
    }
    return body.toString();
  }

  static String abbreviate(String throwableText) {
    return new BufferedReader(new StringReader(throwableText)).lines()
      .map(l -> {
        String abbreviated = l;
        for (Map.Entry<String, String> entry : packageAbbreviation.entrySet()) {
          abbreviated = StringUtils.replace(abbreviated, entry.getKey(), entry.getValue());
        }
        return abbreviated;
      }).collect(Collectors.joining("<br/>\n"));
  }
}
