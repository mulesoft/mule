/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.config.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.internal.config.togglz.user.MuleTogglzArtifactFeatureUser;
import org.togglz.core.user.FeatureUser;


/**
 * Default implementation of {@code FeatureFlaggingService}
 */
public class DefaultFeatureFlaggingService implements FeatureFlaggingService, Disposable {

  private final FeatureUser featureUser;
  private final String artifactId;
  private final MuleTogglzManagedArtifactFeatures features;

  public DefaultFeatureFlaggingService(String artifactId, MuleTogglzManagedArtifactFeatures features) {
    featureUser = new MuleTogglzArtifactFeatureUser(artifactId);
    this.features = features;
    this.artifactId = artifactId;
  }

  @Override
  public boolean isEnabled(Feature feature) {
    if (!features.containsKey(FEATURE_PROVIDER.getOrRegisterRuntimeTogglzFeatureFrom(feature))) {
      throw new MuleRuntimeException(createStaticMessage("Feature %s not registered", feature));
    }

    return features.get(FEATURE_PROVIDER.getRuntimeTogglzFeature(feature)).isEnabled();
  }

  @Override
  public void dispose() {
    features.dispose();
  }
}
