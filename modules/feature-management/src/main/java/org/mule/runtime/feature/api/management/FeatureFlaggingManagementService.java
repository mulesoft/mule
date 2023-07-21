/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.api.management;

/**
 * This is a service to enable/disable runtime features
 *
 * @since 4.5.0
 */
public interface FeatureFlaggingManagementService {

  String PROFILING_FEATURE_MANAGEMENT_SERVICE_KEY = "PROFILING_FEATURE_MANAGEMENT_SERVICE";

  /**
   * disable the feature for an application.
   *
   * @param featureName the feature name to be disabled.
   */
  void disableFeatureFor(String featureName);

  /**
   * enable the feature for an application.
   *
   * @param featureName
   */
  void enableFeatureFor(String featureName);
}
