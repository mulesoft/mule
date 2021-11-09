/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.togglz.user;

import org.togglz.core.user.FeatureUser;

/**
 * Represents a component that may use a Togglz {@link org.togglz.core.Feature}. It can be any logical part of mule. For example,
 * an application, a component location, the runtime itself can be a feature user.
 */
public interface MuleTogglzFeatureUser extends FeatureUser {

  String FEATURE_SCOPE_ATTRIBUTE_KEY = "FEATURE_SCOPE_ATTRIBUTE_KEY";

  /**
   * @return a descriptive name for the user.
   */
  String getName();
}
