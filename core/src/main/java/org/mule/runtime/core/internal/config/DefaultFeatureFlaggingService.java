/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import java.util.Map;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.FeatureFlaggingService;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

/**
 * Default implementation of {@code FeatureFlaggingService}
 */
public class DefaultFeatureFlaggingService implements FeatureFlaggingService {

  private final Map<Feature, Boolean> features;

  public DefaultFeatureFlaggingService() {
    this(emptyMap());
  }

  public DefaultFeatureFlaggingService(Map<Feature, Boolean> features) {
    this.features = features;
  }

  @Override
  public boolean isEnabled(Feature feature) {
    if (!features.containsKey(feature)) {
      throw new MuleRuntimeException(createStaticMessage("Feature %s not registered", feature));
    }
    return features.get(feature);
  }
}
