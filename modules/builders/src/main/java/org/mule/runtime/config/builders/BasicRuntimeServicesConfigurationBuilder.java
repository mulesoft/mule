/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.builders;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.service.scheduler.internal.DefaultSchedulerService;

/**
 * Provides the basic {@link Service}s infrastructure required by the Mule runtime to start in embedded mode.
 * <p>
 * TODO MULE-9655 Remove this when embedded mode supports registering services from the zip file
 *
 * @since 4.0
 */
public class BasicRuntimeServicesConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    MuleRegistry registry = muleContext.getRegistry();
    final DefaultSchedulerService schedulerService = new DefaultSchedulerService();
    schedulerService.start();
    registry.registerObject(schedulerService.getName(), schedulerService);
  }
}
