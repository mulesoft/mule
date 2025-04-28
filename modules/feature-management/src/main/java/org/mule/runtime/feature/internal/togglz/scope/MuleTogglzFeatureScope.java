/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
