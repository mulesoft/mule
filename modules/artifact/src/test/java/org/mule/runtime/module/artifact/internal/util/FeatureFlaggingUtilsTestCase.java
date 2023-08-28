/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.internal.util;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.artifact.internal.util.DeploymentTestingFeatures.ALWAYS_ON_FEATURE;
import static org.mule.runtime.module.artifact.internal.util.DeploymentTestingFeatures.OVERRIDEABLE_FEATURE;
import static org.mule.runtime.module.artifact.internal.util.DeploymentTestingFeatures.OVERRIDEABLE_FEATURE_OVERRIDE;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

@SmallTest
@Story(FEATURE_FLAGGING)
@Issue("MULE-19402")
public class FeatureFlaggingUtilsTestCase {

  private final ArtifactDescriptor artifactDescriptor = mock(ArtifactDescriptor.class);

  @Rule
  public SystemProperty systemProperty = new SystemProperty(OVERRIDEABLE_FEATURE_OVERRIDE, "true");

  @BeforeClass
  public static void registerTestingFeatures() {
    // Ensure that the testing feature flags are registered.
    ALWAYS_ON_FEATURE.getClass();
  }

  @Before
  public void before() {
    when(artifactDescriptor.getName()).thenReturn("test-artifact");
  }

  @Test
  @Issue("MULE-19402")
  public void testFeature() {
    assertThat(FeatureFlaggingUtils.isFeatureEnabled(ALWAYS_ON_FEATURE, artifactDescriptor), is(true));
  }

  @Test
  @Issue("MULE-19402")
  public void testOverriddenFeature() {
    assertThat(FeatureFlaggingUtils.isFeatureEnabled(OVERRIDEABLE_FEATURE, artifactDescriptor), is(true));
  }

}
