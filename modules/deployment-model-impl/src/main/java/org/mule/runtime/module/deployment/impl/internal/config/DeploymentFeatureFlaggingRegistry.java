/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.config;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.CONDITION_CAN_NOT_BE_NULL;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.FEATURE_ALREADY_REGISTERED;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.FEATURE_CAN_NOT_BE_NULL;

/**
 * Service used to register feature flags which will be evaluated by the {@link DeploymentFeatureFlaggingServiceBuilder} at
 * deployment time against a decoupled {@link FeatureContext}, instead of the legacy {@link MuleContext} evaluation.
 * 
 * @see org.mule.runtime.core.internal.config.FeatureFlaggingServiceBuilder
 * @see FeatureFlaggingRegistry
 * @since 4.4.0
 */
public class DeploymentFeatureFlaggingRegistry {

  private final Map<Feature, Predicate<FeatureContext>> configurations = new ConcurrentHashMap<>();

  private static final DeploymentFeatureFlaggingRegistry INSTANCE = new DeploymentFeatureFlaggingRegistry();

  /**
   * Returns a single instance of this service.
   *
   * @return A unique instance of the service
   */
  public static DeploymentFeatureFlaggingRegistry getInstance() {
    return INSTANCE;
  }

  /**
   * Registers a {@link Predicate} associated with a String which represents a given feature.
   * 
   * @see FeatureFlaggingService
   * @param feature   Name representing the registered feature
   * @param condition This predicate will be evaluated at deployment time. The {@link MuleContext} corresponds to the context that
   *                  is being created for this application.
   */
  public void registerFeature(Feature feature, Predicate<FeatureContext> condition) {
    if (feature == null) {
      throw new IllegalArgumentException(FEATURE_CAN_NOT_BE_NULL);
    }
    if (condition == null) {
      throw new IllegalArgumentException(format(CONDITION_CAN_NOT_BE_NULL, feature));
    }
    Predicate<FeatureContext> added = configurations.putIfAbsent(feature, condition);
    if (added != null) {
      throw new IllegalArgumentException(format(FEATURE_ALREADY_REGISTERED, feature));
    }
  }

  /**
   * Returns all the configurations that were registered by using {@link #registerFeature(Feature, Predicate)}
   *
   * @return An unmodifiable map with the registered features.
   */
  public Map<Feature, Predicate<FeatureContext>> getFeatureConfigurations() {
    return unmodifiableMap(configurations);
  }

}
