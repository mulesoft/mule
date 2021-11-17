/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.config;

import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A Read-only Map that represents the features associated to an artifact.
 */
public class MuleTogglzManagedArtifactFeatures implements Map<Feature, FeatureState> {

  private final String artifactId;
  private final Map<Feature, FeatureState> features;

  public MuleTogglzManagedArtifactFeatures(String artifactId,
                                           Map<Feature, FeatureState> features) {
    this.artifactId = artifactId;
    this.features = features;
  }

  @Override
  public int size() {
    return features.size();
  }

  @Override
  public boolean isEmpty() {
    return features.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return features.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return features.containsValue(value);
  }

  @Override
  public FeatureState get(Object key) {
    return features.get(key);
  }

  @Override
  public FeatureState put(Feature key, FeatureState value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FeatureState remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends Feature, ? extends FeatureState> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Feature> keySet() {
    return features.keySet();
  }

  @Override
  public Collection<FeatureState> values() {
    return features.values();
  }

  @Override
  public Set<Entry<Feature, FeatureState>> entrySet() {
    return features.entrySet();
  }

  /**
   * @return the artifact id for the feature
   */
  public String getArtifactId() {
    return artifactId;
  }

  public void dispose() {

  }
}
