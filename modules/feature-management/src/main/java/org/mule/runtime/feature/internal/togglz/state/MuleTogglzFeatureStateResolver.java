/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.togglz.state;

import org.mule.runtime.feature.internal.togglz.scope.MuleTogglzFeatureScope;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

/**
 * Resolves the feature state according to a type of scope
 *
 * @since 4.5.0
 */
public interface MuleTogglzFeatureStateResolver {

  /**
   * @param feature the {@link Feature}
   * @param scope   the {@link MuleTogglzFeatureScope}
   * @return the {@link FeatureState} for the corresponding feature
   */
  FeatureState getFeatureState(Feature feature, MuleTogglzFeatureScope scope);

  /**
   * @param scope        the {@link MuleTogglzFeatureScope}
   * @param featureState the new {@link FeatureState}
   * @return the new {@link FeatureState} for the corresponding feature
   */
  FeatureState setFeatureState(MuleTogglzFeatureScope scope, FeatureState featureState);

  /**
   * Removes the {@link MuleTogglzFeatureState}
   *
   * @param muleFeatureState the {@link MuleTogglzFeatureState}
   */
  void removeFeatureFeature(MuleTogglzFeatureState muleFeatureState);
}
