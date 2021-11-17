/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.config;

import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.getFeature;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.setFeatureState;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;

import org.mule.runtime.feature.api.management.FeatureFlaggingManagementService;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;
import org.togglz.core.repository.FeatureState;

public class DefaultProfilingFeatureManagementService implements FeatureFlaggingManagementService {

  @Override
  public void disableFeatureFor(String featureName, String artifactId) {
    setStatus(featureName, artifactId, false);
  }

  @Override
  public void enableFeatureFor(String featureName, String artifactId) {
    setStatus(featureName, artifactId, true);
  }

  private void setStatus(String featureName, String applicationName, boolean status) {
    withFeatureUser(new MuleTogglzArtifactFeatureUser(applicationName),
                    () -> setFeatureState(new FeatureState(getFeature(featureName), status)));
  }

}
