/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(CLASSLOADING_ISOLATION)
@Story(ARTIFACT_DESCRIPTORS)
public class BundleDescriptorUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void detectsCompatibleBugFixVersion() throws Exception {
    assertThat(isCompatibleVersion("1.0.1", "1.0.0"), is(true));
  }

  @Test
  public void detectsCompatibleMinorVersion() throws Exception {
    assertThat(isCompatibleVersion("1.1.0", "1.0.0"), is(true));
  }

  @Test
  public void detectsIncompatibleBugFixVersion() throws Exception {
    assertThat(isCompatibleVersion("1.0.0", "1.0.1"), is(false));
  }

  @Test
  public void detectsIncompatibleMinorVersion() throws Exception {
    assertThat(isCompatibleVersion("1.0.0", "1.1.0"), is(false));
  }

  @Test
  public void detectsCompatibleMinorVersionWithNoBugFix() throws Exception {
    assertThat(isCompatibleVersion("1.1", "1.0"), is(true));
  }

  @Test
  public void detectsIncompatibleMinorVersionWithNoBugFix() throws Exception {
    assertThat(isCompatibleVersion("1.0", "1.1"), is(false));
  }

  @Test
  public void detectsIncompatibleMajorVersion() throws Exception {
    assertThat(isCompatibleVersion("2.0.0", "1.0.0"), is(false));
  }

  @Test
  public void detectsIncompatibleMajorVersionWithNoMinor() throws Exception {
    assertThat(isCompatibleVersion("2", "1"), is(false));
  }

  @Test
  public void detectsIncompatibleMajorVersionWithNoBugFix() throws Exception {
    assertThat(isCompatibleVersion("2.0", "1.0"), is(false));
  }
}
