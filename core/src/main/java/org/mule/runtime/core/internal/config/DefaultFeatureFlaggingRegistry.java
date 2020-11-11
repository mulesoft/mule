/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

/**
 * Default implementation of {@code FeatureFlaggingRegistry}
 */
public class DefaultFeatureFlaggingRegistry implements FeatureFlaggingRegistry {

  private final Map<String, Predicate<MuleContext>> configurations;

  public DefaultFeatureFlaggingRegistry() {
    configurations = new ConcurrentHashMap<>();
  }

  @Override
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

  @Override
  public Map<String, Predicate<MuleContext>> getFeatureConfigurations() {
    return unmodifiableMap(configurations);
  }
}
