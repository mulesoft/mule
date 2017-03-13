/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.source.polling.PeriodicScheduler;
import org.mule.runtime.core.source.scheduler.SchedulerMessageSource;
import org.mule.runtime.core.source.scheduler.schedule.FixedFrequencyScheduler;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

public class SchedulingMessageSourceFactoryBean extends AbstractAnnotatedObjectFactory<SchedulerMessageSource>
    implements MuleContextAware {

  protected PeriodicScheduler scheduler;
  protected Long frequency;
  private MuleContext muleContext;

  private FixedFrequencyScheduler defaultScheduler() {
    FixedFrequencyScheduler factory = new FixedFrequencyScheduler();
    factory.setFrequency(frequency);
    return factory;
  }

  public void setFrequency(Long frequency) {
    this.frequency = frequency;
  }

  public void setScheduler(PeriodicScheduler scheduler) {
    this.scheduler = scheduler;
  }


  @Override
  public SchedulerMessageSource doGetObject() throws Exception {
    scheduler = scheduler == null ? defaultScheduler() : scheduler;
    return new SchedulerMessageSource(muleContext, scheduler);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
