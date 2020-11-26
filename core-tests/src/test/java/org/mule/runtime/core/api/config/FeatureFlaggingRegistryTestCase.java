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
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.size.SmallTest;

@SmallTest
@Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class FeatureFlaggingRegistryTestCase {

  private static final String SOME_FEATURE = "SOME_FEATURE";

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
    featureFlaggingRegistry.registerFeature(SOME_FEATURE, c -> true);

    assertFeature(SOME_FEATURE, true);
  }


  @Test
  public void registerDisabledFeature() {
    featureFlaggingRegistry.registerFeature(SOME_FEATURE, c -> false);

    assertFeature(SOME_FEATURE, false);
  }

  @Test
  public void failIfInvalidConfig() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(format("Error registering %s: condition must not be null", SOME_FEATURE));

    featureFlaggingRegistry.registerFeature(SOME_FEATURE, null);
  }

  @Test
  public void failIfInvalidFeature() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage("Invalid feature name");

    featureFlaggingRegistry.registerFeature(null, c -> true);
  }

  @Test
  public void failWhenRegisterFeatureTwice() {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(format("Feature %s already registered", SOME_FEATURE));

    featureFlaggingRegistry.registerFeature(SOME_FEATURE, c -> true);
    featureFlaggingRegistry.registerFeature(SOME_FEATURE, c -> false);
  }

  private void assertFeature(String feature, boolean enabled) {
    assertThat(featureFlaggingRegistry.getFeatureConfigurations().keySet(), contains(feature));
    assertThat(featureFlaggingRegistry.getFeatureConfigurations().get(feature).test(null), is(enabled));
  }

}
