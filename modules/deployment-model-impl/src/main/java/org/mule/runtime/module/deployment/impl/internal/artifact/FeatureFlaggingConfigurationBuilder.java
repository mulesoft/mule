/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.core.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.DefaultFeatureFlaggingService;
import org.slf4j.Logger;
import org.mule.runtime.core.api.config.ConfigurationBuilder;

/**
 * {@link ConfigurationBuilder} that registers a {@link FeatureFlaggingService} as {@link CustomService} with per-application
 * feature flags.
 *
 * @see FeatureFlaggingRegistry
 * @see FeatureFlaggingService
 * 
 * @since 4.4.0
 */
public final class FeatureFlaggingConfigurationBuilder extends AbstractConfigurationBuilder {

  private static final Logger LOGGER = getLogger(FeatureFlaggingConfigurationBuilder.class);

  @Override
  protected void doConfigure(MuleContext muleContext) {
    FeatureFlaggingRegistry ffRegistry = FeatureFlaggingRegistry.getInstance();

    Map<String, Boolean> features = new HashMap<>();
    ffRegistry.getFeatureConfigurations().forEach((featureName, p) -> {
      boolean enabled = p.test(muleContext);

      LOGGER.info("Configuring feature {} = {}", featureName, enabled);

      features.put(featureName, enabled);
    });

    muleContext.getCustomizationService()
        .registerCustomServiceImpl(FEATURE_FLAGGING_SERVICE_KEY, new DefaultFeatureFlaggingService(features));
  }

}
