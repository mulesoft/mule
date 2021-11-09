/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.togglz.state;

import org.mule.runtime.core.internal.config.togglz.scope.MuleTogglzFeatureScope;

import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.togglz.core.context.FeatureContext.getFeatureManager;

/**
 * {@link MuleTogglzFeatureStateResolver} for the runtime scope.
 */
public class MuleTogglzRuntimeFeatureStateResolver implements MuleTogglzFeatureStateResolver {

  private final Map<Feature, FeatureState> featureStates = new ConcurrentHashMap<>();
  private final MuleTogglzFeatureStateRepository stateRepository;

  public MuleTogglzRuntimeFeatureStateResolver(MuleTogglzFeatureStateRepository stateRepository) {
    this.stateRepository = stateRepository;
  }

  @Override
  public FeatureState getFeatureState(Feature feature, MuleTogglzFeatureScope scope) {
    return featureStates.computeIfAbsent(feature,
                                         ft -> new MuleTogglzFeatureState(getFeatureManager().getMetaData(ft)
                                             .getDefaultFeatureState(), stateRepository, scope));
  }

  @Override
  public FeatureState setFeatureState(MuleTogglzFeatureScope scope, FeatureState featureState) {
    return featureStates
        .computeIfAbsent(featureState.getFeature(), feature -> new MuleTogglzFeatureState(feature, stateRepository, scope))
        .setEnabled(featureState.isEnabled());
  }

  @Override
  public void removeFeatureFeature(MuleTogglzFeatureState muleFeatureState) {
    featureStates.remove(muleFeatureState);
  }
}
