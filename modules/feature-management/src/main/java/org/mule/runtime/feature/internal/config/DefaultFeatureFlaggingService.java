/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.feature.internal.config;

import static org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.addMuleTogglzRuntimeFeature;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.getTogglzManagedArtifactFeatures;
import static org.mule.runtime.feature.internal.togglz.config.MuleTogglzFeatureFlaggingUtils.withFeatureUser;
import static org.togglz.core.context.FeatureContext.getFeatureManager;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.feature.internal.togglz.config.MuleTogglzManagedArtifactFeatures;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;
import org.togglz.core.Feature;
import org.togglz.core.user.FeatureUser;

import java.util.Map;
import java.util.Set;


/**
 * Default implementation of {@code FeatureFlaggingService}
 */
public class DefaultFeatureFlaggingService implements FeatureFlaggingService, Disposable {

  private final FeatureUser featureUser;
  private final MuleTogglzManagedArtifactFeatures features;

  public DefaultFeatureFlaggingService(String artifactId, Map<org.mule.runtime.api.config.Feature, Boolean> features) {
    registerFeatures(features.keySet());
    featureUser = new MuleTogglzArtifactFeatureUser(artifactId);
    this.features = getTogglzManagedArtifactFeatures(artifactId, features);
  }

  private void registerFeatures(Set<org.mule.runtime.api.config.Feature> features) {
    for (org.mule.runtime.api.config.Feature runtimeFeature : features) {
      addMuleTogglzRuntimeFeature(runtimeFeature);
    }
  }

  @Override
  public boolean isEnabled(org.mule.runtime.api.config.Feature feature) {
    Feature togglzFeature = FEATURE_PROVIDER.getRuntimeTogglzFeature(feature);

    // If the feature state is not precalculated for this context, it is calculated.
    if (!features.containsKey(togglzFeature)) {
      return withFeatureUser(featureUser, () -> getFeatureManager().isActive(togglzFeature));
    }

    return features.get(togglzFeature).isEnabled();
  }

  @Override
  public void dispose() {
    features.dispose();
  }

}
