/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.state;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.feature.internal.togglz.scope.MuleTogglzFeatureScope;

import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

/**
 * This represents a Mule Togglz Feature State.
 *
 * @since 4.5.0
 */
public class MuleTogglzFeatureState extends FeatureState implements Disposable {

  private final MuleTogglzFeatureStateRepository stateRepository;
  private final MuleTogglzFeatureScope scope;

  /**
   * A Mule Togglz Feature Wrapper for a {@link FeatureState}
   *
   * @param feature         the feature
   * @param stateRepository the feature repository
   * @param scope           the {@link MuleTogglzFeatureScope} for the state
   */
  public MuleTogglzFeatureState(Feature feature,
                                MuleTogglzFeatureStateRepository stateRepository,
                                MuleTogglzFeatureScope scope) {
    super(feature);
    this.scope = scope;
    this.stateRepository = stateRepository;
  }

  public MuleTogglzFeatureState(FeatureState defaultFeatureState,
                                MuleTogglzFeatureStateRepository stateRepository,
                                MuleTogglzFeatureScope scope) {
    super(defaultFeatureState.getFeature(), defaultFeatureState.isEnabled());
    this.scope = scope;
    this.stateRepository = stateRepository;
  }

  @Override
  public void dispose() {
    stateRepository.removeFeatureState(this);
  }

  public MuleTogglzFeatureScope getScope() {
    return scope;
  }
}
