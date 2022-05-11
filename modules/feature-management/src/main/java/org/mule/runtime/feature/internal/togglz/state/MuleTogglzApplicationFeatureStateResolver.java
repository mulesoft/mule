/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.state;

import static org.togglz.core.context.FeatureContext.getFeatureManager;

import org.mule.runtime.feature.internal.togglz.scope.MuleTogglzFeatureScope;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A {@link MuleTogglzFeatureStateResolver} for application scopes.
 *
 * @since 4.5.0
 */
public class MuleTogglzApplicationFeatureStateResolver implements MuleTogglzFeatureStateResolver {

  private final Map<MuleTogglzFeatureScope, Map<Feature, FeatureState>> featureStates =
      new ConcurrentHashMap<>();

  private final MuleTogglzFeatureStateRepository stateRepository;

  public MuleTogglzApplicationFeatureStateResolver(MuleTogglzFeatureStateRepository stateRepository) {
    this.stateRepository = stateRepository;
  }

  @Override
  public FeatureState getFeatureState(Feature feature, MuleTogglzFeatureScope scope) {
    return featureStates
        .computeIfAbsent(scope, scp -> new ConcurrentHashMap<>())
        .computeIfAbsent(feature,
                         ft -> new MuleTogglzFeatureState(getFeatureManager().getMetaData(feature).getDefaultFeatureState(),
                                                          stateRepository, scope));
  }

  @Override
  public FeatureState setFeatureState(MuleTogglzFeatureScope scope, FeatureState featureState) {
    return featureStates
        .computeIfAbsent(scope, scp -> new ConcurrentHashMap<>())
        .computeIfAbsent(featureState.getFeature(), feature -> new MuleTogglzFeatureState(feature, stateRepository, scope))
        .setEnabled(featureState.isEnabled());
  }

  @Override
  public void removeFeatureFeature(MuleTogglzFeatureState muleFeatureState) {
    this.featureStates.remove(muleFeatureState);
  }
}
