/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.config;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.config.FeatureFlaggingServiceBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.deployment.impl.internal.config.DeploymentTestingFeatures.TESTING_FEATURE;
import static org.mule.runtime.module.deployment.impl.internal.config.DeploymentTestingFeatures.TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY;

@RunWith(Parameterized.class)
@SmallTest
public class DeploymentFeatureFlaggingServiceBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private MuleContext muleContext;

  @Mock
  private ArtifactDescriptor artifactDescriptor;

  @Mock
  private MuleConfiguration muleConfiguration;

  @Rule
  public SystemProperty systemProperty;

  private final Map<Feature, Predicate<MuleContext>> legacyConfigs;
  private final Map<Feature, Predicate<FeatureContext>> configs;


  private final boolean expected;

  private final Feature feature;

  @Parameterized.Parameters(name = "Feature {0}: enabled={1} and SystemProperty={2} should be {3}")
  public static List<Object[]> parameters() {
    return asList(
                  new Object[] {TESTING_FEATURE, true, null, true},
                  new Object[] {TESTING_FEATURE, false, null, false},

                  new Object[] {TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY, true, "false", false},
                  new Object[] {TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY, true, "true", true},
                  new Object[] {TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY, true, null, true},

                  new Object[] {TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY, false, "false", false},
                  new Object[] {TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY, false, "true", true},
                  new Object[] {TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY, false, null, false});
  }

  @Before
  public void configureContext() {
    when(muleContext.getId()).thenReturn("fake-id");
    when(artifactDescriptor.getName()).thenReturn("fake-name");
  }

  public DeploymentFeatureFlaggingServiceBuilderTestCase(Feature feature, boolean enabled, String systemPropertyValue,
                                                         boolean expected) {
    Map<Feature, Predicate<MuleContext>> legacyConfigs = new HashMap<>();
    Map<Feature, Predicate<FeatureContext>> configs = new HashMap<>();
    legacyConfigs.put(feature, c -> enabled);
    configs.put(feature, c -> enabled);

    this.feature = feature;
    this.legacyConfigs = legacyConfigs;
    this.configs = configs;
    this.expected = expected;

    if (systemPropertyValue != null) {
      feature.getOverridingSystemPropertyName().ifPresent(s -> this.systemProperty = new SystemProperty(s, systemPropertyValue));
    }
  }

  @Test
  public void testBuildUsingLegacyConfigurations() {
    FeatureFlaggingService featureFlaggingService = new FeatureFlaggingServiceBuilder()
        .configurations(legacyConfigs)
        .context(muleContext)
        .build();

    assertThat(featureFlaggingService.isEnabled(feature), is(expected));
  }

  @Test
  public void testBuildUsingConfigurations() {
    FeatureFlaggingService featureFlaggingService = new DeploymentFeatureFlaggingServiceBuilder()
        .withDescriptor(artifactDescriptor)
        .withFeatureContextConfigurations(configs)
        .build();

    assertThat(featureFlaggingService.isEnabled(feature), is(expected));
  }

}
