/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.scheduler.SchedulerConfig.config;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;

import javax.inject.Inject;

import org.springframework.beans.factory.FactoryBean;

/**
 * Builds a base {@link SchedulerConfig} to be provided to the calls to {@link SchedulerService}.
 * 
 * @since 4.0
 */
public class SchedulerBaseConfigFactory implements FactoryBean<SchedulerConfig> {

  @Inject
  private MuleContext muleContext;

  @Override
  public Class<?> getObjectType() {
    return SchedulerConfig.class;
  }

  @Override
  public SchedulerConfig getObject() throws Exception {
    return config().withPrefix(muleContext.getConfiguration().getId())
        .withShutdownTimeout(() -> muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS);
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
