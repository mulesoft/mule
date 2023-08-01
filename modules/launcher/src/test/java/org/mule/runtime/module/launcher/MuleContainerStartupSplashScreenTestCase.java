/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher;

import static org.hamcrest.Matchers.not;

import org.mule.runtime.module.deployment.impl.internal.base.AbstractSplashScreenTestCase;

import static org.mule.runtime.container.api.MuleFoldersUtil.ARTIFACT_PATCHES_FOLDER;
import static org.mule.runtime.core.api.util.FileUtils.newFile;
import static org.mule.tck.junit4.matcher.StringContainsIgnoringLineBreaks.containsStringIgnoringLineBreaks;
import static org.mule.test.allure.AllureConstants.ArtifactPatchingFeature.ARTIFACT_PATCHING;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.List;

import io.qameta.allure.Feature;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;

@Feature(ARTIFACT_PATCHING)
public class MuleContainerStartupSplashScreenTestCase extends AbstractSplashScreenTestCase<MuleContainerStartupSplashScreen> {

  private static final String FIRST_PATCH = "SE-4242-3.8.0.jar";
  private static final String SECOND_PATCH = "SE-9999-3.7.3.jar";
  private static final String MULE_PATCH = "MULE-1345-4.2.0.jar";
  private static final String LIBRARY_JAR = "library.jar";
  private static final String ARTIFACT_PATCH = "SE-12-1.0.0-1.2.3.jar";
  private static final String APIKIT_PATCH = "APIKIT-12-1.2.0-1.3.0.jar";
  private static final String COMPLEX_LOG_PART = "* Mule services:                                                     *\n"
      + "*  - myService.zip                                                   *\n"
      + "* Applied patches:                                                   *\n"
      + "*  - " + MULE_PATCH + "                                             *\n"
      + "*  - " + FIRST_PATCH + "                                               *\n" + "*  - " + SECOND_PATCH
      + "                                               *\n*  - " + LIBRARY_JAR
      + "                                                     *\n"
      + "* Applied artifact patches:                                          *\n"
      + "*  - " + APIKIT_PATCH + "                                       *\n"
      + "*  - " + ARTIFACT_PATCH + "                                           *\n"
      + "* Mule system properties:                                            *\n";

  @BeforeClass
  public static void setUpPatches() {
    File libFolder = newFile(workingDirectory.getRoot(), "lib/patches");
    libFolder.mkdirs();
    newFile(libFolder, FIRST_PATCH).mkdir();
    newFile(libFolder, "library.jar").mkdir();
    newFile(libFolder, SECOND_PATCH).mkdir();
    newFile(libFolder, MULE_PATCH).mkdir();
    File artifactPatchesFolder = newFile(libFolder, ARTIFACT_PATCHES_FOLDER);
    artifactPatchesFolder.mkdirs();
    newFile(artifactPatchesFolder, ARTIFACT_PATCH).mkdir();
    newFile(artifactPatchesFolder, APIKIT_PATCH).mkdir();
    File servicesFolder = newFile(workingDirectory.getRoot(), "services");
    servicesFolder.mkdir();
    newFile(servicesFolder, "myService.zip").mkdir();
  }

  @Before
  public void setUp() {
    splashScreen = new MuleContainerStartupSplashScreen(getAdditionalSplashEntries());
  }

  @Override
  protected void setUpSplashScreen() {
    splashScreen.doBody();
  }

  @Override
  protected Matcher<String> getSimpleLogMatcher() {
    return not(containsStringIgnoringLineBreaks(COMPLEX_LOG_PART));
  }

  @Override
  protected Matcher<String> getComplexLogMatcher() {
    return containsStringIgnoringLineBreaks(COMPLEX_LOG_PART);
  }

  private List<String> getAdditionalSplashEntries() {
    return asList("Additional splash entry 1", "Additional splash entry 2");
  }
}
