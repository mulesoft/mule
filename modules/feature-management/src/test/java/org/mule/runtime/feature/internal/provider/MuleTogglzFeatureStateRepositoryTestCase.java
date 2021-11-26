/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.provider;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;
import static org.mule.runtime.feature.internal.togglz.state.MuleTogglzFeatureStateRepository.FEATURE_IS_NOT_REGISTERED;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import org.mule.runtime.feature.internal.togglz.provider.MuleTogglzFeatureProvider;
import org.mule.runtime.feature.internal.togglz.state.MuleTogglzFeatureStateRepository;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;

import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class MuleTogglzFeatureStateRepositoryTestCase {

  public static final String NON_EXISTING_FEATURE = "NON_EXISTING_FEATURE";
  public static final String EXISTING_FEATURE = "EXISTING_FEATURE";
  public static final String TEST_ARTIFACT = "TEST_ARTIFACT";
  @Mock
  private MuleTogglzFeatureProvider featureProvider;

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Feature notRegisteredFeature;

  @Mock
  private Feature existingFeature;

  @Before
  public void setUp() {
    when(notRegisteredFeature.name()).thenReturn(NON_EXISTING_FEATURE);
    when(featureProvider.getFeature(NON_EXISTING_FEATURE)).thenReturn(null);

    when(existingFeature.name()).thenReturn(EXISTING_FEATURE);
    when(featureProvider.getFeature(EXISTING_FEATURE)).thenReturn(existingFeature);
  }

  @Test
  public void getFeatureForNonExistentStateFails() {
    MuleTogglzFeatureStateRepository featureStateRepository = new MuleTogglzFeatureStateRepository(featureProvider);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format(FEATURE_IS_NOT_REGISTERED, NON_EXISTING_FEATURE));
    featureStateRepository.getFeatureState(notRegisteredFeature);
  }

  @Test
  public void whenStateRegisteredItCanBeRetrievedWithSameInstance() {
    MuleTogglzFeatureStateRepository featureStateRepository = new MuleTogglzFeatureStateRepository(featureProvider);
    featureStateRepository.setFeatureState(new FeatureState(existingFeature, true));
    FeatureState featureStateEnabled = featureStateRepository.getFeatureState(existingFeature);
    assertThat(featureStateEnabled.isEnabled(), is(true));

    featureStateRepository.setFeatureState(new FeatureState(existingFeature, false));
    FeatureState featureStateDisabled = featureStateRepository.getFeatureState(existingFeature);
    assertThat(featureStateDisabled.isEnabled(), is(false));

    assertThat(featureStateEnabled, sameInstance(featureStateDisabled));
  }

  @Test
  public void differentStatesAccordingToScope() {
    final MuleTogglzFeatureStateRepository featureStateRepository = new MuleTogglzFeatureStateRepository(featureProvider);
    withFeatureUser(new MuleTogglzArtifactFeatureUser(TEST_ARTIFACT), () -> {
      featureStateRepository.setFeatureState(new FeatureState(existingFeature, true));
    });

    FeatureState featureState = featureStateRepository.getFeatureState(existingFeature);
    assertThat(featureState.isEnabled(), is(false));

    withFeatureUser(new MuleTogglzArtifactFeatureUser(TEST_ARTIFACT), () -> {
      FeatureState featureStateInArtifactScope = featureStateRepository.getFeatureState(existingFeature);
      assertThat(featureStateInArtifactScope.isEnabled(), is(true));
    });
  }

  @Test
  public void removeFeatureState() {
    final MuleTogglzFeatureStateRepository featureStateRepository = new MuleTogglzFeatureStateRepository(featureProvider);
    withFeatureUser(new MuleTogglzArtifactFeatureUser(TEST_ARTIFACT), () -> {
      featureStateRepository.setFeatureState(new FeatureState(existingFeature, true));
    });

    final FeatureState featureState = featureStateRepository.getFeatureState(existingFeature);
    assertThat(featureState.isEnabled(), is(false));

    withFeatureUser(new MuleTogglzArtifactFeatureUser(TEST_ARTIFACT), () -> {
      featureStateRepository.removeFeatureState(featureState);
    });

    FeatureState featureStateAfterRemoval = featureStateRepository.getFeatureState(existingFeature);
    assertThat(featureStateAfterRemoval.isEnabled(), is(false));
  }
}
