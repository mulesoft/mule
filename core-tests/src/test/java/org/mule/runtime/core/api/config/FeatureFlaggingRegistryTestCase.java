/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.config.Feature.TESTING_FEATURE;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.size.SmallTest;

@SmallTest
@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class FeatureFlaggingRegistryTestCase {

  private FeatureFlaggingRegistry featureFlaggingRegistry;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
  }

  @After
  public void after() {
    featureFlaggingRegistry.clearFeatureConfigurations();
  }

  @Test
  public void registerEnabledFeature() {
    featureFlaggingRegistry.registerFeature(TESTING_FEATURE, c -> true);

    assertFeature(true);
  }


  @Test
  public void registerDisabledFeature() {
    featureFlaggingRegistry.registerFeature(TESTING_FEATURE, c -> false);

    assertFeature(false);
  }

  @Test
  public void failIfInvalidConfig() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(format("Error registering %s: condition must not be null", TESTING_FEATURE.name()));

    featureFlaggingRegistry.registerFeature(TESTING_FEATURE, null);
  }

  @Test
  public void failIfInvalidFeature() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage("Feature can not be null");

    featureFlaggingRegistry.registerFeature(null, c -> true);
  }

  @Test
  public void failWhenRegisterFeatureTwice() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(format("Feature %s already registered", TESTING_FEATURE.name()));

    featureFlaggingRegistry.registerFeature(TESTING_FEATURE, c -> true);
    featureFlaggingRegistry.registerFeature(TESTING_FEATURE, c -> false);
  }

  private void assertFeature(boolean enabled) {
    assertThat(featureFlaggingRegistry.getFeatureConfigurations().keySet(), contains(TESTING_FEATURE));
    assertThat(featureFlaggingRegistry.getFeatureConfigurations().get(TESTING_FEATURE).test(null), is(enabled));
  }

}
