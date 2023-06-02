/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
import static org.mule.tck.config.WeaveExpressionLanguageFactoryServiceProvider.provideDefaultExpressionLanguageFactoryService;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

/**
 * Provides the basic {@link Service}s infrastructure required by the Mule runtime to start in embedded mode.
 *
 * @since 4.0
 */
public class BasicRuntimeServicesConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final SimpleUnitTestSupportSchedulerService simpleUnitTestSupportSchedulerService =
        new SimpleUnitTestSupportSchedulerService();

    try {
      startIfNeeded(simpleUnitTestSupportSchedulerService);
      registerObject(muleContext, simpleUnitTestSupportSchedulerService.getName(), simpleUnitTestSupportSchedulerService);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }

    DefaultExpressionLanguageFactoryService weaveExpressionExecutor = provideDefaultExpressionLanguageFactoryService();
    registerObject(muleContext, weaveExpressionExecutor.getName(), weaveExpressionExecutor);
  }
}
