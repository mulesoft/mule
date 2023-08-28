/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.scope.type;

import org.mule.runtime.feature.internal.togglz.scope.MuleTogglzFeatureScope;

/**
 * Mule Togglz Feature Scope Types.
 *
 * @see MuleTogglzFeatureScope
 *
 * @since 4.5.0
 */
public enum MuleTogglzFeatureScopeType {

  /**
   * Represents a type of {@link MuleTogglzFeatureScope} that corresponds to an artifact.
   */
  ARTIFACT_SCOPE_TYPE,

  /**
   * Represents a type of {@link MuleTogglzFeatureScope} that corresponds to the whole runtime.
   */
  RUNTIME_SCOPE_TYPE;
}
