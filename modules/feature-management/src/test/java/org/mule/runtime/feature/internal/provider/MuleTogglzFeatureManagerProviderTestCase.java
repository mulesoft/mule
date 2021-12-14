/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.provider;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;
import static org.mule.runtime.feature.internal.togglz.config.MuleHotSwitchProfilingFeatures.PROFILING_SERVICE_FEATURE;
import static org.mule.runtime.feature.internal.togglz.state.MuleTogglzFeatureStateRepository.FEATURE_IS_NOT_REGISTERED;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import org.junit.Test;
import org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureManagerProvider;
import org.mule.runtime.feature.internal.togglz.MuleTogglzRuntimeFeature;
import org.mule.runtime.feature.internal.togglz.activation.strategies.MuleTogglzActivatedIfEnabledActivationStrategy;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.spi.ActivationStrategy;
import org.togglz.core.user.FeatureUser;

import java.util.Set;
import java.util.stream.Collectors;

import io.qameta.allure.Story;

@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class MuleTogglzFeatureManagerProviderTestCase {

  public static final String MOCKED_FEATURE_NAME = "mockedFeature";
  public static final String TEST_ARTIFACT_ID = "TEST_ARTIFACT_ID";
  public static final String ANOTHER_TEST_ARTIFACT_ID = "ANOTHER_TEST_ARTIFACT_ID";

  public static final MuleTogglzArtifactFeatureUser ARTIFACT_FEATURE_USER = new MuleTogglzArtifactFeatureUser(TEST_ARTIFACT_ID);
  public static final MuleTogglzArtifactFeatureUser ANOTHER_ARTIFACT_FEATURE_USER =
      new MuleTogglzArtifactFeatureUser(ANOTHER_TEST_ARTIFACT_ID);

  @Rule
  public ExpectedException expectedException = none();

  FeatureManager featureManager = new MuleTogglzFeatureManagerProvider().getFeatureManager();

  @Test
  public void muleActivationStrategies() {
    assertThat(featureManager.getActivationStrategies().stream()
        .map(ActivationStrategy::getId).collect(Collectors.toList()),
               hasItem(MuleTogglzActivatedIfEnabledActivationStrategy.ID));
  }

  @Test
  public void activateFeatureUserCanBeRetrievedThroughThread() {
    FeatureUser featureUser = mock(FeatureUser.class);
    withFeatureUser(featureUser, () -> assertThat(featureManager.getCurrentFeatureUser(), equalTo(featureUser)));
  }

  @Test
  public void initialFeatures() {
    Set<Feature> features = featureManager.getFeatures();
    assertThat(features, hasItem(PROFILING_SERVICE_FEATURE));
  }

  @Test
  public void featureNotExisting() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException
        .expectMessage(format(FEATURE_IS_NOT_REGISTERED, MOCKED_FEATURE_NAME));
    Feature feature = mock(Feature.class);
    when(feature.name()).thenReturn(MOCKED_FEATURE_NAME);
    featureManager.getFeatureState(feature);
  }

  @Test
  public void registerRuntimeFeature() {
    org.mule.runtime.api.config.Feature runtimeFeature = mock(org.mule.runtime.api.config.Feature.class);
    MuleTogglzRuntimeFeature togglzRuntimeFeature = FEATURE_PROVIDER.getOrRegisterRuntimeTogglzFeatureFrom(runtimeFeature);
    assertThat(featureManager.getFeatureState(togglzRuntimeFeature).isEnabled(), is(false));
    featureManager.setFeatureState(new FeatureState(togglzRuntimeFeature, true));
    assertThat(featureManager.getFeatureState(togglzRuntimeFeature).isEnabled(), is(true));
  }

  @Test
  public void featureStatusDependsOnCurrentUserScope() {
    org.mule.runtime.api.config.Feature runtimeFeature = mock(org.mule.runtime.api.config.Feature.class);
    MuleTogglzRuntimeFeature togglzRuntimeFeature = FEATURE_PROVIDER.getOrRegisterRuntimeTogglzFeatureFrom(runtimeFeature);

    withFeatureUser(ARTIFACT_FEATURE_USER, () -> {
      FeatureState featureState = featureManager.getFeatureState(togglzRuntimeFeature);
      assertThat(featureState.isEnabled(), is(false));

      featureManager.setFeatureState(new FeatureState(togglzRuntimeFeature, true));
      assertThat(featureState.isEnabled(), is(true));
    });

    withFeatureUser(ANOTHER_ARTIFACT_FEATURE_USER,
                    () -> assertThat(featureManager.getFeatureState(togglzRuntimeFeature).isEnabled(), is(false)));
  }
}
