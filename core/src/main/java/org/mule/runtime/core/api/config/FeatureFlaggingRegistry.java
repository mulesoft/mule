/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.core.api.MuleContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;

/**
 * Service used to register feature flags which will be evaluated at deployment time. For example:
 * 
 * <code>
 *     // register a feature always "on"
 *     FeatureFlaggingRegistry.getInstance().registerFeature("some feature", c -> true);
 * </code>
 * 
 * <code>
 *    &#64;Inject
 *    &#64;Named(FEATURE_FLAGGING_SERVICE_KEY)
 *    private FeatureFlaggingService featureFlaggingService;
 *    // ....
 *    
 *    if (featureFlaggingService.isEnabled("some feature")) {
 *        // ...
 *    }
 * </code>
 * 
 * @see FeatureFlaggingRegistry
 * @since 4.4.0, 4.3.0, 4.2.3
 */
public class FeatureFlaggingRegistry {

  public static final String FEATURE_CAN_NOT_BE_NULL = "Feature can not be null";
  public static final String FEATURE_ALREADY_REGISTERED = "Feature %s already registered";
  public static final String CONDITION_CAN_NOT_BE_NULL = "Error registering %s: condition can not be null";

  private final Map<Feature, Predicate<MuleContext>> configurations = new ConcurrentHashMap<>();

  private static final FeatureFlaggingRegistry INSTANCE = new FeatureFlaggingRegistry();

  /**
   * Returns a single instance of this service.
   * 
   * @return A unique instance of the service
   */
  public static FeatureFlaggingRegistry getInstance() {
    return INSTANCE;
  }

  private FeatureFlaggingRegistry() {}

  /**
   * Registers a {@link Predicate} associated with a String which represents a given feature. The {@link Predicate} will be
   * evaluated at deployment time, exposing all the features through a per-application overridden {@link FeatureFlaggingService}
   * 
   * @see FeatureFlaggingService
   * 
   * @param feature Name representing the registered feature
   * @param condition This predicate will be evaluated at deployment time. The {@link MuleContext} corresponds to the context that
   *        is being created for this application.
   */
  public void registerFeature(Feature feature, Predicate<MuleContext> condition) {
    if (feature == null) {
      throw new IllegalArgumentException(FEATURE_CAN_NOT_BE_NULL);
    }

    if (condition == null) {
      throw new IllegalArgumentException(format(CONDITION_CAN_NOT_BE_NULL, feature));
    }

    Predicate<MuleContext> added = configurations.putIfAbsent(feature, condition);
    if (added != null) {
      throw new IllegalArgumentException(format(FEATURE_ALREADY_REGISTERED, feature));
    }
  }

  /**
   * Returns all the configurations that were registered by using {@link #registerFeature(Feature, Predicate)}
   * 
   * @return An unmodifiable map with the registered features.
   */
  public Map<Feature, Predicate<MuleContext>> getFeatureConfigurations() {
    return unmodifiableMap(configurations);
  }

  /**
   * Cleans up all the previously registered configurations. This method is meant to be used just for test purposes.
   */
  protected void clearFeatureConfigurations() {
    configurations.clear();
  }
}
