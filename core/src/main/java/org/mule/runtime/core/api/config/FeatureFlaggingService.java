/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

/**
 * @TODO document
 * 
 * @since 4.4.0
 */
public interface FeatureFlaggingService {

  String FEATURE_FLAGGING_SERVICE_KEY = "_featureFlaggingService";

  boolean isEnabled(String feature);
}
