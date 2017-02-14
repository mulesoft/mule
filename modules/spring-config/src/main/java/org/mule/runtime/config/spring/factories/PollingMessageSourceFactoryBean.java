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
import org.mule.runtime.core.api.source.polling.PeriodicScheduler;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencyScheduler;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

public class PollingMessageSourceFactoryBean extends AbstractAnnotatedObjectFactory<PollingMessageSource>
    implements MuleContextAware {

  protected PeriodicScheduler scheduler;
  protected Processor messageProcessor;
  protected MessageProcessorPollingOverride override;
  protected Long frequency;
  private MuleContext muleContext;

  private FixedFrequencyScheduler defaultScheduler() {
    FixedFrequencyScheduler factory = new FixedFrequencyScheduler();
    factory.setFrequency(frequency);
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

  public void setScheduler(PeriodicScheduler scheduler) {
    this.scheduler = scheduler;
  }


  @Override
  public PollingMessageSource doGetObject() throws Exception {
    scheduler = scheduler == null ? defaultScheduler() : scheduler;
    override = override != null ? this.override : new MessageProcessorPollingOverride.NullOverride();
    return new PollingMessageSource(muleContext, messageProcessor, override, scheduler);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
