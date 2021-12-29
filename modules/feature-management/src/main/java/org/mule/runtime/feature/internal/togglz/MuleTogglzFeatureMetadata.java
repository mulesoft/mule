/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import org.mule.runtime.feature.internal.togglz.activation.strategies.MuleTogglzActivatedIfEnabledActivationStrategy;
import org.togglz.core.Feature;
import org.togglz.core.metadata.FeatureGroup;
import org.togglz.core.metadata.FeatureMetaData;
import org.togglz.core.repository.FeatureState;

import java.util.Map;
import java.util.Set;


/**
 * A {@link FeatureMetaData} used for the features defined by the runtime.
 *
 * @since 4.5.0
 */
public class MuleTogglzFeatureMetadata implements FeatureMetaData {

  private final FeatureState defaultFeatureState;

  public MuleTogglzFeatureMetadata(Feature newFeature) {
    this(newFeature, false);
  }

  public MuleTogglzFeatureMetadata(Feature newFeature, boolean enabled) {
    this.defaultFeatureState = new FeatureState(newFeature, enabled);
    this.defaultFeatureState.setStrategyId(MuleTogglzActivatedIfEnabledActivationStrategy.ID);
  }

  @Override
  public String getLabel() {
    return "Mule Runtime defined Feature";
  }

  @Override
  public FeatureState getDefaultFeatureState() {
    return defaultFeatureState.copy();
  }

  @Override
  public Set<FeatureGroup> getGroups() {
    return emptySet();
  }

  @Override
  public Map<String, String> getAttributes() {
    return emptyMap();
  }
}
