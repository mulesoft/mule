/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.config;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.tck.UnitTestSchedulerService;

/**
 * Registers services instances into the {@link MuleRegistry} of a {@link MuleContext}.
 * 
 * @since 4.0
 */
public class RegisterServicesConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  public void doConfigure(MuleContext muleContext) throws Exception {
    MuleRegistry registry = muleContext.getRegistry();
    registry.registerObject("UnitTestSchedulerService", new UnitTestSchedulerService());
  }

}
