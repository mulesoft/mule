/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.builders;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A support class for {@link org.mule.runtime.core.api.config.ConfigurationBuilder} implementations that handles the logic of
 * creating config arrays and {@link java.util.Properties} arguments
 *
 * @see org.mule.runtime.core.api.config.ConfigurationBuilder
 */
public abstract class AbstractConfigurationBuilder implements ConfigurationBuilder {

  protected final List<ServiceConfigurator> serviceConfigurators = new ArrayList<>();

  /**
   * Adds a service configurator to be used on the context being built.
   *
   * @param serviceConfigurator service to add. Non null.
   */
  @Override
  public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
    checkArgument(serviceConfigurator != null, "serviceConfigurator cannot be null");
    serviceConfigurators.add(serviceConfigurator);
  }

  /**
   * Will configure a MuleContext object based on the builders configuration settings. This method will delegate the actual
   * processing to {@link #doConfigure(org.mule.runtime.core.api.MuleContext)}
   *
   * @param muleContext The current {@link org.mule.runtime.core.api.MuleContext}
   * @throws ConfigurationException if the configuration fails i.e. an object cannot be created or initialised properly
   */
  @Override
  public void configure(MuleContext muleContext) throws ConfigurationException {
    try {
      doConfigure(muleContext);
      applyLifecycle(muleContext.getLifecycleManager());
    } catch (Exception e) {
      throw new ConfigurationException(e);
    }
  }

  /**
   * Will configure a MuleContext based on the configuration provided. The configuration will be set on the
   * {@link org.mule.runtime.core.api.config.ConfigurationBuilder} implementation as bean properties before this method has been
   * called.
   *
   * @param muleContext The current {@link org.mule.runtime.core.api.MuleContext}
   * @throws ConfigurationException if the configuration fails i.e. an object cannot be created or initialised properly
   */
  protected abstract void doConfigure(MuleContext muleContext) throws Exception;

  /**
   * Allows a configuration builder to check and customise the lifecycle of objects in the registry being used. The ONLY time a
   * user should implement this method is if the underlying container for the Registry is an IoC container had manages it's own
   * lifecycle. If this is the case the lifecycle manager can be used to call the next lifecycle method on all the objects. For
   * example for the Spring Registry only Initialise and Dispose phase is handled by Spring. The Start and Stop phases are handled
   * by Mule by calling- <code>
   * // If the MuleContext is started, start all objects in the new Registry.
   *  if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME))
   *  {
   *      lifecycleManager.applyPhase(registry.lookupObjects(Object.class), Startable.PHASE_NAME);
   *  }
   * </code>
   * 
   * @param lifecycleManager the lifecycleManager for the current context
   * @throws Exception if anything goes wrong. Usually this is an exeption bubbled up from calling a lifecycle method on an object
   *         in the registry
   */
  protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception {
    // by default do nothing
  }
}
