/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.togglz.core.repository.FeatureState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.core.api.config.TestingFeatures.TESTING_FEATURE;
import static org.mule.runtime.core.internal.config.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

@RunWith(Parameterized.class)
@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class DefaultFeatureFlaggingServiceTestCase {

  public static final String ARTIFACT_ID = "artifactId";
  private final FeatureFlaggingService featureFlaggingService;

  private final Feature feature;
  private final boolean enabled;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Parameters(name = "Feature \"{1}\" should be {2}")
  public static List<Object[]> parameters() {
    return asList(
                  new Object[] {buildFeatureConfigurations(TESTING_FEATURE, false), TESTING_FEATURE, false, null},
                  new Object[] {buildFeatureConfigurations(TESTING_FEATURE, true), TESTING_FEATURE, true, null},
                  new Object[] {buildFeatureConfigurations(), TESTING_FEATURE, false, (Consumer<ExpectedException>) (e -> {
                    e.expect(MuleRuntimeException.class);
                    e.expectMessage(format("Feature %s not registered", TESTING_FEATURE.name()));
                  })});

  }

  public DefaultFeatureFlaggingServiceTestCase(Map<Feature, Boolean> featureConfigurations, Feature feature, boolean enabled,
                                               Consumer<ExpectedException> configureExpected) {

    this.feature = feature;
    this.enabled = enabled;

    Map<org.togglz.core.Feature, FeatureState> featureStates = new HashMap<>();
    featureConfigurations
        .forEach((feat, enbld) -> featureStates
            .put(FEATURE_PROVIDER.getOrRegisterRuntimeTogglzFeatureFrom(feat),
                 new FeatureState(FEATURE_PROVIDER.getOrRegisterRuntimeTogglzFeatureFrom(feat), enbld)));

    featureFlaggingService =
        new DefaultFeatureFlaggingService(ARTIFACT_ID, new MuleTogglzManagedArtifactFeatures(ARTIFACT_ID, featureStates));
    if (configureExpected != null) {
      configureExpected.accept(expectedException);
    }
  }

  @Test
  public void testCase() {
    assertThat(featureFlaggingService.isEnabled(feature), is(enabled));
  }

  private static Map<Feature, Boolean> buildFeatureConfigurations(Object... values) {
    assertThat("Values must be even", values.length % 2, is(0));

    Map<Feature, Boolean> m = new HashMap<>();

    for (int i = 0; i < values.length; i += 2) {
      m.put((Feature) values[i], (Boolean) values[i + 1]);
    }

    return m;
  }

}

