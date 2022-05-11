/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.config;

import org.mule.runtime.feature.internal.togglz.state.MuleTogglzFeatureState;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.util.Map;

/**
 * Represents the features associated to an artifact.
 *
 * @since 4.5.0
 */
public class MuleTogglzManagedArtifactFeatures {

  private final String artifactId;
  private final Map<Feature, FeatureState> features;

  public MuleTogglzManagedArtifactFeatures(String artifactId,
                                           Map<Feature, FeatureState> features) {
    this.artifactId = artifactId;
    this.features = features;
  }

  public boolean containsKey(Object key) {
    return features.containsKey(key);
  }

  /**
   * @return the artifact id for the feature
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * disposes the feature states.
   */
  public void dispose() {
    for (FeatureState featureState : features.values()) {
      if (featureState instanceof MuleTogglzFeatureState) {
        ((MuleTogglzFeatureState) featureState).dispose();
      }
    }
  }

  /**
   * @param togglzFeature the {@link Feature}
   * @return the corresponding FeatureS
   */
  public FeatureState get(Feature togglzFeature) {
    return features.get(togglzFeature);
  }
}
