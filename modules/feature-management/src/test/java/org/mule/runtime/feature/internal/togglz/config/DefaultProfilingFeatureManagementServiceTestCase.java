/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.getFeature;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.getFeatureState;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzProfilingFeatures.PROFILING_SERVICE_FEATURE;

import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;



@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class DefaultProfilingFeatureManagementServiceTestCase {

  public static final String ARTIFACT_TEST_ID = "ARTIFACT_TEST_ID";

  @Test
  public void enableAndDisableFeatures() {
    MuleTogglzArtifactFeatureUser featureUser = new MuleTogglzArtifactFeatureUser(ARTIFACT_TEST_ID);

    DefaultProfilingFeatureManagementService profilingFeatureManagementService = new DefaultProfilingFeatureManagementService();
    profilingFeatureManagementService.enableFeatureFor(PROFILING_SERVICE_FEATURE.name(), ARTIFACT_TEST_ID);

    withFeatureUser(featureUser, () -> {
      assertThat(getFeatureState(getFeature(PROFILING_SERVICE_FEATURE.name())).isEnabled(), is(true));
    });

    profilingFeatureManagementService.disableFeatureFor(PROFILING_SERVICE_FEATURE.name(), ARTIFACT_TEST_ID);

    withFeatureUser(featureUser, () -> {
      assertThat(getFeatureState(getFeature(PROFILING_SERVICE_FEATURE.name())).isEnabled(), is(false));
    });
  }

}
