/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.provider;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;

import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.feature.internal.togglz.MuleTogglzRuntimeFeature;
import org.mule.runtime.feature.internal.togglz.provider.DefaultMuleTogglzFeatureProvider;
import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;

import java.util.Optional;

@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class MuleTogglzFeatureProviderTestCase {

  public static final String DUMMY_CONSUMER_IDENTIFIER = "DUMMY_CONSUMER_IDENTIFIER";
  public static final String CONSUMER_NAME = "consumerName";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultMuleTogglzFeatureProvider featureProvider;

  @Before
  public void setUp() {
    featureProvider = new DefaultMuleTogglzFeatureProvider(TestProfilingFeatures.class);
  }

  @Test
  public void enumFeaturesAreRegisteredCorrectly() {
    assertThat(featureProvider.getFeatures(), hasItem(TestProfilingFeatures.ENABLED_TESTING_FEATURE));
    assertThat(featureProvider.getFeatures(), hasItem(TestProfilingFeatures.DISABLED_TESTING_FEATURE));
    assertThat(featureProvider.getMetaData(TestProfilingFeatures.ENABLED_TESTING_FEATURE).getDefaultFeatureState().isEnabled(),
               is(true));
    assertThat(featureProvider.getMetaData(TestProfilingFeatures.DISABLED_TESTING_FEATURE).getDefaultFeatureState().isEnabled(),
               is(false));
  }

  @Test
  public void muleRuntimeFeaturesAreRegisteredCorrectly() {
    MuleTogglzRuntimeFeature feature =
        featureProvider.getOrRegisterRuntimeTogglzFeatureFrom(TestRuntimeProfilingFeatures.TEST_RUNTIME_FEATURE);
    assertThat(featureProvider.getFeatures(), hasItem(feature));
    // Registration of a runtime feature should be disabled in Togglz by default. When the configuration is evaluated the
    // FeatureState
    // should change corresponding to the application.
    assertThat(featureProvider.getMetaData(feature).getDefaultFeatureState().isEnabled(), is(false));
    assertThat(featureProvider.getFeature(TestRuntimeProfilingFeatures.TEST_RUNTIME_FEATURE.toString()),
               equalTo(feature));
    assertThat(feature.getRuntimeFeature(), equalTo(TestRuntimeProfilingFeatures.TEST_RUNTIME_FEATURE));
  }


  @Test
  public void runtimeFeaturesRegisteredTwiceReturnSameInstance() {
    org.mule.runtime.api.config.Feature mockedFeature = mock(org.mule.runtime.api.config.Feature.class);
    MuleTogglzRuntimeFeature feature = featureProvider.getOrRegisterRuntimeTogglzFeatureFrom(mockedFeature);
    MuleTogglzRuntimeFeature featureAgain = featureProvider.getOrRegisterRuntimeTogglzFeatureFrom(mockedFeature);
    assertThat(feature, sameInstance(featureAgain));
  }

  /**
   * Testing {@link org.mule.runtime.api.profiling.type.ProfilingEventType}
   */
  private enum TestProfilingEventType implements ProfilingEventType<ProfilingEventContext> {

    TEST_PROFILING_EVENT_TYPE;

    public static final String TEST_ID = "TEST_ID";

    @Override
    public String getProfilingEventTypeIdentifier() {
      return TEST_ID;
    }

    @Override
    public String getProfilingEventTypeNamespace() {
      return TEST_ID;
    }
  }


  /**
   * Testing {@link Feature}'s.
   */
  private enum TestProfilingFeatures implements Feature {
    @EnabledByDefault
    ENABLED_TESTING_FEATURE, DISABLED_TESTING_FEATURE
  }

  /**
   * Testing {@link org.mule.runtime.api.config.Feature}'s.
   */
  private enum TestRuntimeProfilingFeatures implements org.mule.runtime.api.config.Feature {

    TEST_RUNTIME_FEATURE("Test Runtime Feature", "issueId", "true", "overridingProperty");

    private final String description;
    private final String issueId;
    private final String enabledByDefaultSince;
    private final String overridingSystemPropertyName;

    TestRuntimeProfilingFeatures(String description, String issueId, String enabledByDefaultSince,
                                 String overridingSystemPropertyName) {
      this.description = description;
      this.issueId = issueId;
      this.enabledByDefaultSince = enabledByDefaultSince;
      this.overridingSystemPropertyName = overridingSystemPropertyName;
    }

    public String getDescription() {
      return this.description;
    }

    public String getIssueId() {
      return this.issueId;
    }

    public String getSince() {
      return this.getEnabledByDefaultSince();
    }

    public String getEnabledByDefaultSince() {
      return this.enabledByDefaultSince;
    }

    public Optional<String> getOverridingSystemPropertyName() {
      return ofNullable(this.overridingSystemPropertyName);
    }

  }

}
