/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config;

import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.getFeature;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.setFeatureState;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.feature.api.management.FeatureFlaggingManagementService;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;
import org.togglz.core.repository.FeatureState;

import javax.inject.Inject;

/**
 * The default implementation for a service to enable/disable runtime features.
 *
 * @since 4.5.0
 */
public class DefaultFeatureManagementService implements FeatureFlaggingManagementService {

  @Inject
  private MuleContext muleContext;

  @Override
  public void disableFeatureFor(String featureName) {
    setStatus(featureName, false);
  }

  @Override
  public void enableFeatureFor(String featureName) {
    setStatus(featureName, true);
  }

  private void setStatus(String featureName, boolean status) {
    withFeatureUser(new MuleTogglzArtifactFeatureUser(muleContext.getConfiguration().getId()),
                    () -> setFeatureState(new FeatureState(getFeature(featureName), status)));
  }
}
