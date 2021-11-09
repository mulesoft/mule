/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.management;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.core.internal.config.management.MuleTogglzProfilingFeatures.PROFILING_SERVICE_FEATURE;
import static org.mule.runtime.core.internal.config.FeatureFlaggingUtils.getFeatureState;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import org.mule.runtime.core.internal.config.FeatureFlaggingUtils;
import org.mule.runtime.core.internal.config.togglz.user.MuleTogglzArtifactFeatureUser;

import io.qameta.allure.Story;
import org.junit.Test;

@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class DefaultProfilingFeatureManagementServiceTestCase {

  public static final String ARTIFACT_TEST_ID = "ARTIFACT_TEST_ID";

  @Test
  public void enableAndDisableFeatures() {
    MuleTogglzArtifactFeatureUser featureUser = new MuleTogglzArtifactFeatureUser(ARTIFACT_TEST_ID);

    DefaultProfilingFeatureManagementService profilingFeatureManagementService = new DefaultProfilingFeatureManagementService();
    profilingFeatureManagementService.enableFeatureFor(PROFILING_SERVICE_FEATURE.name(), ARTIFACT_TEST_ID);

    FeatureFlaggingUtils.withFeatureUser(featureUser, () -> {
      assertThat(getFeatureState(FeatureFlaggingUtils.getFeature(PROFILING_SERVICE_FEATURE.name())).isEnabled(), is(true));
    });

    profilingFeatureManagementService.disableFeatureFor(PROFILING_SERVICE_FEATURE.name(), ARTIFACT_TEST_ID);

    FeatureFlaggingUtils.withFeatureUser(featureUser, () -> {
      assertThat(getFeatureState(FeatureFlaggingUtils.getFeature(PROFILING_SERVICE_FEATURE.name())).isEnabled(), is(false));
    });
  }

}
