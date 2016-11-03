/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.concurrent.Executor;

import javax.resource.spi.work.Work;

/**
 * Processes {@link Event}'s asynchronously using a {@link Scheduler} to schedule asynchronous processing of the next
 * {@link Processor}. The next {@link Processor} is therefore be executed in a different thread regardless of the exchange-pattern
 * configured on the inbound endpoint. If a transaction is present then an exception is thrown.
 */
public class AsyncInterceptingMessageProcessor extends BaseAsyncInterceptingMessageProcessor {

  public static final String SYNCHRONOUS_EVENT_ERROR_MESSAGE = "Unable to process a synchronous event asynchronously";

  protected Scheduler workScheduler;

  @Override
  protected boolean isProcessAsync(Event event) throws MuleException {
    if (!canProcessAsync(event)) {
      throw new DefaultMuleException(createStaticMessage(SYNCHRONOUS_EVENT_ERROR_MESSAGE));
    }
    return canProcessAsync(event);
  }

  @Override
  protected void doProcessNextAsync(Work work) throws MuleException {
    workScheduler.submit(work);
  }

  public void setScheduler(Scheduler workScheduler) {
    this.workScheduler = workScheduler;
  }

  @Override
  protected Executor resolveExecutor() {
    return workScheduler;
  }
}
