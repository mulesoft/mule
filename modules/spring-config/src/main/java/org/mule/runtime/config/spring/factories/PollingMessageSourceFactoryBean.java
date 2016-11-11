/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.polling.ScheduledPollFactory;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencyScheduledPollFactory;

import org.springframework.beans.factory.FactoryBean;

public class PollingMessageSourceFactoryBean implements FactoryBean, MuleContextAware {

  protected ScheduledPollFactory schedulerFactory;
  protected Processor messageProcessor;
  protected MessageProcessorPollingOverride override;
  protected Long frequency;
  private MuleContext muleContext;

  private FixedFrequencyScheduledPollFactory defaultSchedulerFactory() {
    FixedFrequencyScheduledPollFactory factory = new FixedFrequencyScheduledPollFactory();
    factory.setFrequency(frequency);
    factory.setMuleContext(muleContext);
    return factory;
  }

  public void setMessageProcessor(Processor messageProcessor) {
    this.messageProcessor = messageProcessor;
  }

  public void setOverride(MessageProcessorPollingOverride override) {
    this.override = override;
  }

  public void setFrequency(Long frequency) {
    this.frequency = frequency;
  }

  public void setSchedulerFactory(ScheduledPollFactory schedulerFactory) {
    this.schedulerFactory = schedulerFactory;
  }


  @Override
  public Object getObject() throws Exception {
    schedulerFactory = schedulerFactory == null ? defaultSchedulerFactory() : schedulerFactory;
    override = override != null ? this.override : new MessageProcessorPollingOverride.NullOverride();
    return new PollingMessageSource(muleContext, messageProcessor, override, schedulerFactory);
  }

  @Override
  public Class<?> getObjectType() {
    return PollingMessageSource.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
