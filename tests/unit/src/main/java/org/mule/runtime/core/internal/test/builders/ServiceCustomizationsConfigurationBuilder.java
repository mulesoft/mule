/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.test.builders;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.registry.Registry;

import java.util.Map;

/**
 * This is useful for registering any Map of objects with the {@link Registry} via the {@link ConfigurationBuilder} interface.
 * 
 * @since 4.5
 */
public final class ServiceCustomizationsConfigurationBuilder extends AbstractConfigurationBuilder {

  private final Map<String, Object> objects;

  public ServiceCustomizationsConfigurationBuilder(Map<String, Object> objects) {
    this.objects = objects;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    if (objects != null && objects.size() > 0) {
      CustomizationService customizationService = muleContext.getCustomizationService();
      objects.entrySet()
          .forEach(e -> customizationService.registerCustomServiceImpl(e.getKey(), e.getValue(), true));
    }
  }
}
