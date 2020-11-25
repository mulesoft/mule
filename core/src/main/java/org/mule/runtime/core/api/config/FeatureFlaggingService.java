/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

/**
 * This service exposes the features that were flagged based on the configurations registered through the
 * {@link FeatureFlaggingRegistry}. These configurations will be evaluated when an application is deployed, which means that each
 * application will have its own set of flags independently of the rest of the applications deployed in a given runtime.
 * 
 * @see FeatureFlaggingRegistry
 * @since 4.4.0
 */
public interface FeatureFlaggingService {

  String FEATURE_FLAGGING_SERVICE_KEY = "core.featureFlaggingService";

  boolean isEnabled(String feature);
}
