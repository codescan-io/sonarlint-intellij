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

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.util.Consumer;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.SonarLintPlugin;
import org.sonarlint.intellij.common.util.SonarLintUtils;

// Inspired from https://github.com/openclover/clover/blob/master/clover-idea/src/com/atlassian/clover/idea/util/BlameClover.java
public class BlameSonarSource extends ErrorReportSubmitter {
  static final int MAX_URI_LENGTH = 2000;
  private static final int BUG_FAULT_CATEGORY_ID = 6;
  private static final String INTELLIJ_TAG = "intellij";
  private static final String COMMUNITY_ROOT_URL = "https://community.sonarsource.com/";
  private static final String COMMUNITY_FAULT_CATEGORY_URL = COMMUNITY_ROOT_URL + "tags/c/" + BUG_FAULT_CATEGORY_ID + "/" + INTELLIJ_TAG;
  private static final String COMMUNITY_NEW_TOPIC_URL = COMMUNITY_ROOT_URL + "new-topic"
    + "?title=Error+in+CodeScan+for+IntelliJ"
    + "&category_id=" + BUG_FAULT_CATEGORY_ID
    + "&tags=sonarlint," + INTELLIJ_TAG;

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
    return "Report to SonarSource";
  }

  @Override
  public boolean submit(@NotNull IdeaLoggingEvent[] events,
    @Nullable String additionalInfo,
    @NotNull Component parentComponent,
    @NotNull Consumer<SubmittedReportInfo> consumer) {
    String body = buildBody(events, additionalInfo);
    BrowserUtil.browse(getReportWithBodyUrl(body));
    consumer.consume(new SubmittedReportInfo(COMMUNITY_FAULT_CATEGORY_URL, "community support thread", SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
    return true;
  }

  @NotNull
  static String buildBody(@NotNull IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo) {
    StringBuilder body = new StringBuilder();
    body.append("Environment:\n");
    body.append("* Java: ").append(System.getProperty("java.vendor")).append(" ").append(System.getProperty("java.version")).append("\n");
    body.append("* OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.arch")).append("\n");
    body.append("* IDE: ").append(ApplicationInfo.getInstance().getFullApplicationName()).append("\n");
    body.append("* CodeScan: ").append(SonarLintUtils.getService(SonarLintPlugin.class).getVersion()).append("\n");
    body.append("\n");
    if (additionalInfo != null) {
      body.append(additionalInfo);
      body.append("\n");
    }
    for (IdeaLoggingEvent ideaLoggingEvent : events) {
      final String message = ideaLoggingEvent.getMessage();
      if (StringUtils.isNotBlank(message)) {
        body.append(message).append("\n");
      }
      final String throwableText = ideaLoggingEvent.getThrowableText();
      if (StringUtils.isNotBlank(throwableText)) {
        body.append("\n```\n");
        body.append(abbreviate(throwableText));
        body.append("\n```\n\n");
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
      }).collect(Collectors.joining("\n"));

  }

  String getReportWithBodyUrl(String description) {
    final String urlStart = COMMUNITY_NEW_TOPIC_URL + "&body=";
    final int charsLeft = MAX_URI_LENGTH - urlStart.length();

    return urlStart + getBoundedEncodedString(description, charsLeft);
  }

  String getBoundedEncodedString(String description, int maxLen) {
    try {
      String encoded = URLEncoder.encode(description, "UTF-8");
      while (encoded.length() > maxLen) {
        int lastNewline = description.lastIndexOf('\n');
        if (lastNewline == -1) {
          return "";
        }
        description = description.substring(0, lastNewline);
        encoded = URLEncoder.encode(description, "UTF-8");
      }

      return encoded;
    } catch (UnsupportedEncodingException e) {
      return "";
    }

  }
}
