/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.artifact;

import static org.mule.runtime.module.service.internal.artifact.LibraryByJavaVersion.resolveJvmDependantLibs;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_CONFIGURATION_LOADER;

import static java.io.File.separator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;

import org.mule.runtime.module.service.internal.artifact.LibraryByJavaVersion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Stories({@Story(CLASSLOADER_CONFIGURATION_LOADER), @Story(CLASSLOADER_CONFIGURATION)})
public class LibraryByJavaVersionTestCase {

  private final File someLib_1_1 = new File("someFolder" + separator + "someLib-1.1.jar");
  private final File someLib_1_2 = new File("someFolder" + separator + "someLib-1.2.jar");
  private final File someOtherLib_1_1 = new File("someFolder" + separator + "someOtherLib-1.1.jar");

  @Test
  public void singleLib() {
    final List<LibraryByJavaVersion> libs = new ArrayList<>();
    libs.add(new LibraryByJavaVersion(11, someLib_1_1));

    final List<File> resolvedLibs = resolveJvmDependantLibs(11, libs);
    assertThat(resolvedLibs, hasSize(1));
    assertThat(resolvedLibs.get(0), is(someLib_1_1));
  }

  @Test
  public void noLibForJvm() {
    final List<LibraryByJavaVersion> libs = new ArrayList<>();
    libs.add(new LibraryByJavaVersion(17, someLib_1_1));

    assertThat(resolveJvmDependantLibs(11, libs), is(emptyIterable()));
  }

  @Test
  public void sameLibForDifferentJvms() {
    final List<LibraryByJavaVersion> libs = new ArrayList<>();
    libs.add(new LibraryByJavaVersion(11, someLib_1_1));
    libs.add(new LibraryByJavaVersion(17, someLib_1_2));

    final List<File> resolvedLibs = resolveJvmDependantLibs(17, libs);
    assertThat(resolvedLibs, hasSize(1));
    assertThat(resolvedLibs.get(0), is(someLib_1_2));
  }

  @Test
  public void sameLibForDifferentJvmsLowerApplicable() {
    final List<LibraryByJavaVersion> libs = new ArrayList<>();
    libs.add(new LibraryByJavaVersion(11, someLib_1_1));
    libs.add(new LibraryByJavaVersion(17, someLib_1_2));

    final List<File> resolvedLibs = resolveJvmDependantLibs(11, libs);
    assertThat(resolvedLibs, hasSize(1));
    assertThat(resolvedLibs.get(0), is(someLib_1_1));
  }

  @Test
  public void differentLibForDifferentJvms() {
    final List<LibraryByJavaVersion> libs = new ArrayList<>();
    libs.add(new LibraryByJavaVersion(11, someLib_1_1));
    libs.add(new LibraryByJavaVersion(17, someOtherLib_1_1));

    final List<File> resolvedLibs = resolveJvmDependantLibs(17, libs);
    assertThat(resolvedLibs, hasSize(2));
    assertThat(resolvedLibs.get(0), is(someLib_1_1));
    assertThat(resolvedLibs.get(1), is(someOtherLib_1_1));
  }
}
