/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.source.scheduler.PeriodicScheduler;
import org.mule.runtime.core.internal.source.scheduler.DefaultSchedulerMessageSource;
import org.mule.runtime.core.api.source.scheduler.FixedFrequencyScheduler;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

public class SchedulingMessageSourceFactoryBean extends AbstractComponentFactory<DefaultSchedulerMessageSource>
    implements MuleContextAware {

  private static final long DEFAULT_FREQUENCY = 1000l;
  protected PeriodicScheduler scheduler;
  private MuleContext muleContext;

  private FixedFrequencyScheduler defaultScheduler() {
    FixedFrequencyScheduler factory = new FixedFrequencyScheduler();
    factory.setFrequency(DEFAULT_FREQUENCY);
    return factory;
  }

  public void setScheduler(PeriodicScheduler scheduler) {
    this.scheduler = scheduler;
  }


  @Override
  public DefaultSchedulerMessageSource doGetObject() throws Exception {
    scheduler = scheduler == null ? defaultScheduler() : scheduler;
    return new DefaultSchedulerMessageSource(muleContext, scheduler);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
