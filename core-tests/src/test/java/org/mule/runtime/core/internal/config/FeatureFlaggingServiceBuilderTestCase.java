/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.TestingFeatures.TESTING_FEATURE;
import static org.mule.runtime.core.api.config.TestingFeatures.TESTING_FEATURE_OVERRIDDEN_WITH_SYSTEM_PROPERTY;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.api.config.Feature;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

@RunWith(Parameterized.class)
@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
@SmallTest
public class FeatureFlaggingServiceBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private MuleContext muleContext;

  @Mock
  private MuleConfiguration muleConfiguration;

  @Rule
  public SystemProperty systemProperty;

  private final Map<Feature, Predicate<MuleContext>> configs;

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
    when(muleConfiguration.getId()).thenReturn("fake-id");
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
  }

  public FeatureFlaggingServiceBuilderTestCase(Feature feature, boolean enabled, String systemPropertyValue, boolean expected) {
    Map<Feature, Predicate<MuleContext>> configs = new HashMap<>();
    configs.put(feature, c -> enabled);

    this.feature = feature;
    this.configs = configs;
    this.expected = expected;

    if (systemPropertyValue != null) {
      feature.getOverridingSystemPropertyName().ifPresent(s -> this.systemProperty = new SystemProperty(s, systemPropertyValue));
    }
  }

  @Test
  public void testBuild() {
    FeatureFlaggingService featureFlaggingService = new FeatureFlaggingServiceBuilder()
        .configurations(configs)
        .context(muleContext)
        .build();

    assertThat(featureFlaggingService.isEnabled(feature), is(expected));

  }

}
