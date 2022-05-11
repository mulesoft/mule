/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import static org.mule.runtime.core.api.config.TestingFeatures.TESTING_FEATURE;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.feature.internal.config.DefaultFeatureFlaggingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class DefaultFeatureFlaggingServiceTestCase {

  public static final String ARTIFACT_ID = "artifactId";
  private final boolean registered;
  private FeatureFlaggingService featureFlaggingService;
  private final Feature feature;
  private final boolean enabled;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Parameters(name = "Feature \"{1}\" should be {2}")
  public static List<Object[]> parameters() {
    return asList(
                  new Object[] {TESTING_FEATURE, false, true},
                  new Object[] {TESTING_FEATURE, true, true},
                  new Object[] {TESTING_FEATURE, false, true});

  }

  public DefaultFeatureFlaggingServiceTestCase(Feature feature, boolean enabled,
                                               boolean registered) {

    this.feature = feature;
    this.enabled = enabled;
    this.registered = registered;
  }

  @Before
  public void before() {
    featureFlaggingService =
        new DefaultFeatureFlaggingService(ARTIFACT_ID, getFeaturesStates());

    if (!registered) {
      expectedException.expect(MuleRuntimeException.class);
      expectedException.expectMessage(format("Feature %s not registered", TESTING_FEATURE.name()));
    }
  }

  private Map<Feature, Boolean> getFeaturesStates() {
    if (!registered) {
      return emptyMap();
    }
    return new HashMap<Feature, Boolean>() {

      {
        put(feature, enabled);
      }
    };
  }

  @Test
  public void testCase() {
    assertThat(featureFlaggingService.isEnabled(feature), is(enabled));
  }

}
