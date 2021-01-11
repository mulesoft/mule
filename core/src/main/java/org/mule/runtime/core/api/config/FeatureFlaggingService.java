/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

import org.mule.runtime.api.config.Feature;

/**
 * This service exposes the features that were flagged based on the configurations registered through the
 * {@link FeatureFlaggingRegistry}. These configurations will be evaluated when an application is deployed, which means that each
 * application will have its own set of flags independently of the rest of the applications deployed in a given runtime.
 * 
 * @see FeatureFlaggingRegistry
 * @since 4.4.0, 4.3.0, 4.2.3
 */
public interface FeatureFlaggingService {

  String FEATURE_FLAGGING_SERVICE_KEY = "core.featureFlaggingService";

  /**
   * Inform if a given @{link feature} is enabled for the current context.
   * 
   * @see FeatureFlaggingRegistry
   * @see FeatureFlaggingService
   * 
   * @param feature The name of the feature being queried, as per was registered through {@link FeatureFlaggingRegistry}
   * 
   * @return a boolean indicating if the features is enabled for the current execution context
   */
  boolean isEnabled(Feature feature);
}
