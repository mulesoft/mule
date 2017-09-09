/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;

import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;

/**
 * Provides the basic {@link Service}s infrastructure required by the Mule runtime to start in embedded mode.
 *
 * @since 4.0
 */
public class BasicRuntimeServicesConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    MuleRegistry registry = muleContext.getRegistry();

    new SpiServiceRegistry().lookupProviders(Service.class, BasicRuntimeServicesConfigurationBuilder.class.getClassLoader())
        .forEach(service -> {
          try {
            startIfNeeded(service);
            registry.registerObject(service.getName(), service);
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        });

    DefaultExpressionLanguageFactoryService weaveExpressionExecutor = new WeaveDefaultExpressionLanguageFactoryService();
    registry.registerObject(weaveExpressionExecutor.getName(), weaveExpressionExecutor);
  }
}
