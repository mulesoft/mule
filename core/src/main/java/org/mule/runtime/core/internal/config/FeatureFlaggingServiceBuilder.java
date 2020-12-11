/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.slf4j.Logger;

/**
 * This builder creates a {@link FeatureFlaggingService}.
 *
 * @see FeatureFlaggingService
 * 
 * @since 4.4.0
 */
public final class FeatureFlaggingServiceBuilder {

  private static final Logger LOGGER = getLogger(FeatureFlaggingServiceBuilder.class);

  private MuleContext context;

  private Map<Feature, Predicate<MuleContext>> configurations;

  public FeatureFlaggingServiceBuilder context(MuleContext context) {
    this.context = context;
    return this;
  }

  public FeatureFlaggingServiceBuilder configurations(Map<Feature, Predicate<MuleContext>> cofigurations) {
    this.configurations = cofigurations;
    return this;
  }

  public FeatureFlaggingService build() {
    Map<Feature, Boolean> features = new EnumMap<>(Feature.class);
    LOGGER.debug("Configuring feature flags...");

    final String id = context.getConfiguration().getId();
    configurations.forEach((feature, p) -> {
      boolean enabled = p.test(context);

      LOGGER.debug("Setting feature {} = {} for artifact [{}]", feature, enabled, id);

      features.put(feature, enabled);
    });

    return new DefaultFeatureFlaggingService(features);

  }

}
