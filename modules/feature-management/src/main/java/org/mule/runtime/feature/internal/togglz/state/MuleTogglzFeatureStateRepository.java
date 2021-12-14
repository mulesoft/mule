/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.state;

import static org.mule.runtime.feature.internal.togglz.scope.type.MuleTogglzFeatureScopeType.ARTIFACT_SCOPE_TYPE;
import static org.mule.runtime.feature.internal.togglz.user.MuleFeatureUser.FEATURE_SCOPE_ATTRIBUTE_KEY;
import static java.util.Collections.singletonMap;
import static org.togglz.core.context.FeatureContext.getFeatureManager;

import org.mule.runtime.feature.internal.togglz.provider.MuleTogglzFeatureProvider;
import org.mule.runtime.feature.internal.togglz.scope.MuleTogglzFeatureScope;
import org.mule.runtime.feature.internal.togglz.scope.type.MuleTogglzFeatureScopeType;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.user.FeatureUser;

import java.util.Map;

/**
 * A Togglz {@link StateRepository} for the Mule runtime
 *
 * @since 4.5.0
 */
public class MuleTogglzFeatureStateRepository implements StateRepository {

  private final Map<MuleTogglzFeatureScopeType, MuleTogglzFeatureStateResolver> featureResolvers =
      singletonMap(ARTIFACT_SCOPE_TYPE, new MuleTogglzApplicationFeatureStateResolver(MuleTogglzFeatureStateRepository.this));

  public static final String INVALID_FEATURE_SCOPE_ATTRIBUTE = "Invalid feature scope attribute";
  public static final String FEATURE_IS_NOT_REGISTERED = "Feature '%s' is not registered";

  private final MuleTogglzFeatureStateResolver defaultFeatureStateResolver =
      new MuleTogglzRuntimeFeatureStateResolver(MuleTogglzFeatureStateRepository.this);
  private final MuleTogglzFeatureProvider featureProvider;

  public MuleTogglzFeatureStateRepository(MuleTogglzFeatureProvider featureProvider) {
    this.featureProvider = featureProvider;
  }

  @Override
  public FeatureState getFeatureState(Feature feature) {
    if (featureProvider.getFeature(feature.name()) == null) {
      throw new IllegalArgumentException(String.format(FEATURE_IS_NOT_REGISTERED, feature.name()));
    }

    MuleTogglzFeatureScope scope = getScope();

    return getFeatureStateResolver(scope).getFeatureState(feature, scope);
  }

  @Override
  public void setFeatureState(FeatureState featureState) {
    MuleTogglzFeatureScope scope = getScope();
    getFeatureStateResolver(scope).setFeatureState(scope, featureState);
  }

  private MuleTogglzFeatureScope getScope() {
    FeatureUser featureUser = getFeatureManager().getCurrentFeatureUser();
    if (featureUser == null) {
      return null;
    }

    Object scope = featureUser.getAttribute(FEATURE_SCOPE_ATTRIBUTE_KEY);

    if (scope != null && !(scope instanceof MuleTogglzFeatureScope)) {
      throw new IllegalStateException(INVALID_FEATURE_SCOPE_ATTRIBUTE);
    }

    return (MuleTogglzFeatureScope) scope;
  }

  public void removeFeatureState(FeatureState muleFeatureState) {
    if (!(muleFeatureState instanceof MuleTogglzFeatureState)) {
      throw new IllegalArgumentException("The Mule Feature State Repository can only remove Mule Feature States");
    }

    MuleTogglzFeatureState muleTogglzFeatureState = (MuleTogglzFeatureState) muleFeatureState;
    getFeatureStateResolver(muleTogglzFeatureState.getScope()).removeFeatureFeature(muleTogglzFeatureState);
  }

  private MuleTogglzFeatureStateResolver getFeatureStateResolver(MuleTogglzFeatureScope scope) {
    if (scope == null) {
      return defaultFeatureStateResolver;
    } else {
      return featureResolvers.getOrDefault(scope.getScopeType(), defaultFeatureStateResolver);
    }
  }
}
