/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2020 SonarSource
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
package org.sonarlint.intellij.analysis;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.rules.TempDirectory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.sonarlint.intellij.AbstractSonarLintLightTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class JavaAnalysisConfiguratorTests extends AbstractSonarLintLightTests {

  private static final Path FAKE_JDK_ROOT_PATH = Paths.get("src/test/resources/fake_jdk/").toAbsolutePath();
  private static final String MY_EXPORTED_LIB_JAR = "myExportedLib.jar";
  private static final String MY_NON_EXPORTED_LIB_JAR = "myNonExportedLib.jar";

  @Rule
  public TempDirectory tempDir = new TempDirectory();

  private JavaAnalysisConfigurator underTest = new JavaAnalysisConfigurator();
  private File exportedLibFile;
  private File nonExportedLibFile;

  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return new DefaultLightProjectDescriptor() {
      @Override
      public Sdk getSdk() {
        return addRtJarTo(IdeaTestUtil.getMockJdk18());
      }

      @Override
      public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
        super.configureModule(module, model, contentEntry);
        Module dependentModule = createModule(module.getProject(), FileUtil.join(FileUtil.getTempDirectory(), "dependent.iml"));
        model.addModuleOrderEntry(dependentModule);
        try {
          exportedLibFile = tempDir.newFile(MY_EXPORTED_LIB_JAR);
          nonExportedLibFile = tempDir.newFile(MY_NON_EXPORTED_LIB_JAR);
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
        ModuleRootModificationUtil.updateModel(dependentModule, dependentModel -> {
          PsiTestUtil.addLibrary(dependentModel, "myNonExportedLib", nonExportedLibFile.getParent(), nonExportedLibFile.getName());
          Library myExportedLib = PsiTestUtil.addLibrary(dependentModel, "myExportedLib", exportedLibFile.getParent(), exportedLibFile.getName());
          final LibraryOrderEntry libraryOrderEntry = dependentModel.findLibraryOrderEntry(myExportedLib);
          libraryOrderEntry.setExported(true);
        });
      }
    };
  }

  @Test
  public void testSourceAndTarget_with_default_target() {
    CompilerConfiguration.getInstance(getProject()).setBytecodeTargetLevel(getModule(), null);

    IdeaTestUtil.setModuleLanguageLevel(getModule(), LanguageLevel.JDK_1_8);
    assertThat(underTest.configure(getModule())).contains(entry("sonar.java.source", "8"), entry("sonar.java.target", "8"));

    IdeaTestUtil.setModuleLanguageLevel(getModule(), LanguageLevel.JDK_1_9);
    assertThat(underTest.configure(getModule())).contains(entry("sonar.java.source", "9"), entry("sonar.java.target", "9"));
  }

  @Test
  public void testSourceAndTarget_with_different_target() {
    IdeaTestUtil.setModuleLanguageLevel(getModule(), LanguageLevel.JDK_1_8);
    CompilerConfiguration.getInstance(getProject()).setBytecodeTargetLevel(getModule(), "7");
    assertThat(underTest.configure(getModule())).contains(entry("sonar.java.source", "8"), entry("sonar.java.target", "7"));
  }

  @Test
  public void testAddJdkClasspath() {
    final Map<String, String> props = underTest.configure(getModule());
    assertThat(props).containsKeys("sonar.java.libraries", "sonar.java.test.libraries");
    assertThat(Stream.of(props.get("sonar.java.libraries").split(",")).map(Paths::get))
      .contains(
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/rt.jar"),
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/another.jar"));
    assertThat(Stream.of(props.get("sonar.java.test.libraries").split(",")).map(Paths::get))
      .contains(
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/rt.jar"),
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/another.jar"));
  }

  @Test
  public void testAddExportedDependentModuleLibs() {
    final Map<String, String> props = underTest.configure(getModule());
    assertThat(props).containsKeys("sonar.java.libraries", "sonar.java.test.libraries");
    assertThat(Stream.of(props.get("sonar.java.libraries").split(",")).map(Paths::get))
      .containsExactly(
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/rt.jar"),
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/another.jar"),
        exportedLibFile.toPath());
    assertThat(Stream.of(props.get("sonar.java.test.libraries").split(",")).map(Paths::get))
      .containsExactly(
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/rt.jar"),
        FAKE_JDK_ROOT_PATH.resolve("jdk1.8/lib/another.jar"),
        exportedLibFile.toPath());
  }

  private static Sdk addRtJarTo(@NotNull Sdk jdk) {
    try {
      jdk = (Sdk) jdk.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    SdkModificator sdkModificator = jdk.getSdkModificator();
    sdkModificator.addRoot(findJar("jdk1.8/lib/rt.jar"), OrderRootType.CLASSES);
    sdkModificator.addRoot(findJar("jdk1.8/lib/another.jar"), OrderRootType.CLASSES);
    sdkModificator.commitChanges();
    return jdk;
  }

  @NotNull
  private static VirtualFile findJar(@NotNull String name) {
    Path path = FAKE_JDK_ROOT_PATH.resolve(name);
    VirtualFile file = VfsTestUtil.findFileByCaseSensitivePath(path.toString());
    VirtualFile jar = JarFileSystem.getInstance().getJarRootForLocalFile(file);
    assert jar != null : "no .jar for: " + path;
    return jar;
  }
}
