/*
 * SonarLint for IntelliJ IDEA
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
package org.sonarlint.intellij.common.util;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.PlatformUtils;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaResourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;

public class SonarLintUtils {

  private static final Logger LOG = Logger.getInstance(SonarLintUtils.class);
  private static final String[] SONARCLOUD_ALIAS = {"https://app.codescan.io"};

  private SonarLintUtils() {
    // Utility class
  }

  public static <T> T getService(Class<T> clazz) {
    T t = ServiceManager.getService(clazz);
    if (t == null) {
      LOG.error("Could not find service: " + clazz.getName());
      throw new IllegalArgumentException("Class not found: " + clazz.getName());
    }

    return t;
  }

  public static <T> T getService(@NotNull Project project, Class<T> clazz) {
    T t = ServiceManager.getService(project, clazz);
    if (t == null) {
      LOG.error("Could not find service: " + clazz.getName());
      throw new IllegalArgumentException("Class not found: " + clazz.getName());
    }

    return t;
  }

  public static <T> T getService(@NotNull Module module, Class<T> clazz) {
    T t = ModuleServiceManager.getService(module, clazz);
    if (t == null) {
      LOG.error("Could not find service: " + clazz.getName());
      throw new IllegalArgumentException("Class not found: " + clazz.getName());
    }

    return t;
  }

  public static boolean isSonarCloudAlias(@Nullable String url) {
    return Arrays.asList(SONARCLOUD_ALIAS).contains(url);
  }

  public static boolean isEmpty(@Nullable String str) {
    return str == null || str.isEmpty();
  }

  public static boolean isBlank(@Nullable String str) {
    return str == null || str.trim().isEmpty();
  }

  public static boolean equalsIgnoringTrailingSlash(String aString, String anotherString) {
    return withTrailingSlash(aString).equals(withTrailingSlash(anotherString));
  }

  private static String withTrailingSlash(String str) {
    if (!str.endsWith("/")) {
      return str + '/';
    }
    return str;
  }

  public static Image iconToImage(Icon icon) {
    if (icon instanceof ImageIcon) {
      return ((ImageIcon) icon).getImage();
    } else {
      int w = icon.getIconWidth();
      int h = icon.getIconHeight();
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice gd = ge.getDefaultScreenDevice();
      GraphicsConfiguration gc = gd.getDefaultConfiguration();
      BufferedImage image = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
      Graphics2D g = image.createGraphics();
      icon.paintIcon(null, g, 0, 0);
      g.dispose();
      return image;
    }
  }

  /**
   * FileEditorManager#getSelectedFiles does not work as expected. In split editors, the order of the files does not change depending
   * on which one of the split editors is selected.
   * This seems to work well with split editors.
   */
  @CheckForNull
  public static VirtualFile getSelectedFile(Project project) {
    if (project.isDisposed()) {
      return null;
    }
    FileEditorManager editorManager = FileEditorManager.getInstance(project);

    Editor editor = editorManager.getSelectedTextEditor();
    if (editor != null) {
      Document doc = editor.getDocument();
      FileDocumentManager docManager = FileDocumentManager.getInstance();
      return docManager.getFile(doc);
    }

    return null;
  }

  public static boolean isGeneratedSource(SourceFolder sourceFolder) {
    // copied from JavaProjectRootsUtil. Don't use that class because it's not available in other flavors of Intellij
    JavaSourceRootProperties properties = sourceFolder.getJpsElement().getProperties(JavaModuleSourceRootTypes.SOURCES);
    JavaResourceRootProperties resourceProperties = sourceFolder.getJpsElement().getProperties(JavaModuleSourceRootTypes.RESOURCES);
    return (properties != null && properties.isForGeneratedSources()) || (resourceProperties != null && resourceProperties.isForGeneratedSources());
  }

  @Nullable
  public static SourceFolder getSourceFolder(@CheckForNull VirtualFile source, Module module) {
    if (source == null) {
      return null;
    }
    for (ContentEntry entry : ModuleRootManager.getInstance(module).getContentEntries()) {
      for (SourceFolder folder : entry.getSourceFolders()) {
        if (source.equals(folder.getFile())) {
          return folder;
        }
      }
    }
    return null;
  }

  public static boolean isJavaResource(SourceFolder source) {
    return JavaModuleSourceRootTypes.RESOURCES.contains(source.getRootType());
  }

  public static String pluralize(String str, long i) {
    if (i == 1) {
      return str;
    }
    return str + "s";
  }

  public static boolean isPhpLanguageRegistered() {
    return Language.findLanguageByID("PHP") != null;
  }

  public static boolean isPhpFile(@NotNull PsiFile file) {
    return "php".equalsIgnoreCase(file.getFileType().getName());
  }

  @CheckForNull
  public static String getIdeVersionForTelemetry() {
    String ideVersion = null;
    try {
      ApplicationInfo appInfo = getAppInfo();
      ideVersion = appInfo.getVersionName() + " " + appInfo.getFullVersion();
      String edition = ApplicationNamesInfo.getInstance().getEditionName();
      if (edition != null) {
        ideVersion += " (" + edition + ")";
      }
    } catch (NullPointerException noAppInfo) {
      return null;
    }
    return ideVersion;
  }

  private static ApplicationInfo getAppInfo() {
    return ApplicationInfo.getInstance();
  }

  public static boolean isTaintVulnerabilitiesEnabled() {
    // No Taint Vulnerabilities in C/C++ for the time being
    return !PlatformUtils.isCLion();
  }

  public static boolean isModuleLevelBindingEnabled() {
    return !PlatformUtils.isRider() && !PlatformUtils.isCLion() && !PlatformUtils.isAppCode();
  }
}
