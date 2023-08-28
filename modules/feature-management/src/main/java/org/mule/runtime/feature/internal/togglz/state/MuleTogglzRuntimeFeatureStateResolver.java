/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.togglz.state;

import static org.togglz.core.context.FeatureContext.getFeatureManager;

import org.mule.runtime.feature.internal.togglz.scope.MuleTogglzFeatureScope;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link MuleTogglzFeatureStateResolver} for the runtime scope.
 *
 * @since 4.5.0
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
    this.featureStates.remove(muleFeatureState.getFeature());
  }
}
