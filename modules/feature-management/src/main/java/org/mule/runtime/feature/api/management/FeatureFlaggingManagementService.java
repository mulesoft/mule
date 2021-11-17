/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.api.management;

/**
 * This is a service to enable/disable runtime features
 *
 * @since 4.5.0
 */
public interface FeatureFlaggingManagementService {

  String REGISTRY_KEY = "PROFILING_FEATURE_MANAGEMENT_SERVICE";

  /**
   * disable the feature for an application.
   *
   * @param featureName     the feature name to be disabled.
   * @param applicationName the application name.
   */
  void disableFeatureFor(String featureName, String applicationName);

  /**
   * disable the feature for an application.
   *
   * @param featureName
   * @param applicationName
   */
  void enableFeatureFor(String featureName, String applicationName);
}
