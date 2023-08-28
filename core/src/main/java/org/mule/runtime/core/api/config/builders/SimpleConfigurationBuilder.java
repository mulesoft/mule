/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config.builders;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.Registry;

import java.util.Map;

/**
 * This simple {@link ConfigurationBuilder} implementation. This is useful for registering any Map of objects with the
 * {@link Registry} via the {@link ConfigurationBuilder} interface. This is useful for example for the registration of "startup
 * properties" which are provided at startup and then used to fill "property placeholders" in other configuration mechanisms such
 * as XML.
 */
public final class SimpleConfigurationBuilder extends AbstractConfigurationBuilder {

  protected Map<String, ?> objects;

  public SimpleConfigurationBuilder(Map<String, ?> objects) {
    this.objects = objects;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    if (objects != null && objects.size() > 0) {
      ((MuleContextWithRegistry) muleContext).getRegistry().registerObjects((Map<String, Object>) objects);
    }
  }
}
