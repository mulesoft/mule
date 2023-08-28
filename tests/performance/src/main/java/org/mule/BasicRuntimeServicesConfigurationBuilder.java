/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
