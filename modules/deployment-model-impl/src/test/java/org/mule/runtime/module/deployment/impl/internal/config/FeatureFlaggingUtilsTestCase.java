package org.mule.runtime.module.deployment.impl.internal.config;

import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.size.SmallTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.deployment.impl.internal.config.DeploymentTestingFeatures.ALWAYS_ON_FEATURE;

@SmallTest
@Story("")
public class FeatureFlaggingUtilsTestCase {

  private final ArtifactDescriptor artifactDescriptor = mock(ArtifactDescriptor.class);

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
  public void testFeatureUsingUtil() {
    assertThat(FeatureFlaggingUtils.isFeatureEnabled(ALWAYS_ON_FEATURE, artifactDescriptor), is(true));
  }

}
