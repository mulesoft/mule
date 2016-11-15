/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.config;

import static org.mockito.Mockito.spy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

/**
 * Registers services instances into the {@link MuleRegistry} of a {@link MuleContext}.
 * <p>
 * This is to be used only in tests that do not leverage the service injection mechanism.
 * 
 * @since 4.0
 */
public class TestServicesConfigurationBuilder extends AbstractConfigurationBuilder {

  final SimpleUnitTestSupportSchedulerService schedulerService = new SimpleUnitTestSupportSchedulerService();

  @Override
  public void doConfigure(MuleContext muleContext) throws Exception {
    MuleRegistry registry = muleContext.getRegistry();
    registry.registerObject(schedulerService.getName(), spy(schedulerService));
  }

  public void stopServices() throws MuleException {
    schedulerService.stop();
  }

}
