/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.togglz.scope;

import org.mule.runtime.feature.internal.togglz.scope.type.MuleTogglzFeatureScopeType;

/**
 * The scope for the state of a {@link org.togglz.core.Feature}. In each scope, the
 * {@link org.togglz.core.repository.FeatureState} can vary.
 *
 * @see org.mule.runtime.feature.internal.togglz.state.MuleTogglzFeatureStateRepository
 *
 * @since 4.5.0
 */
public interface MuleTogglzFeatureScope {

  /**
   * The scope type.
   * 
   * @see MuleTogglzFeatureScopeType
   *
   * @return the scope type
   */
  MuleTogglzFeatureScopeType getScopeType();

}
