/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

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

  private final Map<String, Predicate<MuleContext>> configurations = new ConcurrentHashMap<>();

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
  public void registerFeature(String feature, Predicate<MuleContext> condition) {
    if (isNullOrEmpty(feature)) {
      throw new MuleRuntimeException(createStaticMessage("Invalid feature name"));
    }

    if (condition == null) {
      throw new MuleRuntimeException(createStaticMessage("Error registering %s: condition must not be null", feature));
    }

    Predicate<MuleContext> added = configurations.putIfAbsent(feature, condition);
    if (added != null) {
      throw new MuleRuntimeException(createStaticMessage("Feature %s already registered", feature));
    }
  }

  /**
   * Returns all the configurations that were registered by using {@link #registerFeature(String, Predicate)}
   * 
   * @return An unmodifiable map with the registered features.
   */
  public Map<String, Predicate<MuleContext>> getFeatureConfigurations() {
    return unmodifiableMap(configurations);
  }

  /**
   * Cleans up all the previously registered configurations. This method is meant to be used just for test purposes.
   */
  protected void clearFeatureConfigurations() {
    configurations.clear();
  }
}
