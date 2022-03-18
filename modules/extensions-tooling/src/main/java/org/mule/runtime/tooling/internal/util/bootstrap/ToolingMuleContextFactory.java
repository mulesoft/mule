/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.internal.util.bootstrap;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.tooling.api.connectivity.ToolingActivityContext;
import org.mule.runtime.tooling.internal.service.scheduler.ToolingSchedulerService;

public class ToolingMuleContextFactory {

  public MuleContext createMuleContext(ToolingActivityContext context) throws MuleException {
    return createMuleContext(context, true);
  }

  public MuleContext createMuleContext(ToolingActivityContext context, boolean start) throws MuleException {
    MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext(getConfigurationBuilders(context));

    if (start) {
      muleContext.start();
    }

    return muleContext;
  }

  private ConfigurationBuilder[] getConfigurationBuilders(ToolingActivityContext context) {
    return new ConfigurationBuilder[] {
        new ToolingConfigurationBuilder(context),
        getServicesConfigurationBuilder(),
        getExtensionManagerConfigurationBuilder()
    };
  }

  private ConfigurationBuilder getExtensionManagerConfigurationBuilder() {
    return new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        ExtensionManager extensionManager = new DefaultExtensionManager();
        DefaultMuleContext ctx = (DefaultMuleContext) muleContext;
        ctx.setExtensionManager(extensionManager);
        ctx.getRegistry().registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);
      }
    };
  }

  private ConfigurationBuilder getServicesConfigurationBuilder() {
    return new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) {
        MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
        SchedulerService schedulerService = new ToolingSchedulerService();
        try {
          registry.registerObject(getServiceId(SchedulerService.class, schedulerService), schedulerService);
        } catch (RegistrationException e) {
          throw new MuleRuntimeException(e);
        }
      }
    };
  }

  private <T extends Service> String getServiceId(Class<T> contractClass, T service) {
    return service.getName() + "-" + contractClass.getSimpleName();
  }

}
