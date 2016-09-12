/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.work;

import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createExecutionTemplate;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.ResponseTimeoutException;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link AbstractMuleEventWork} that executes a {@link Processor} using this work's event. Instances of this
 * class can be used in a "fire and forget" fashion or the {@link #getResult(long, TimeUnit)} method can be used to wait for
 * background processing to finish. For cases when used in this latter manner, this class provides semmantics similar to
 * {@link Future#get(long, TimeUnit)} but with some differences around exception handling, cancellation, etc (see
 * {@link #getResult(long, TimeUnit)}).
 * 
 * @since 3.5.0
 */
public class ProcessingMuleEventWork extends AbstractMuleEventWork {

  private MessageProcessorExecutionTemplate messageProcessorExecutionTemplate;
  private final CountDownLatch latch = new CountDownLatch(1);
  private final Processor messageProcessor;
  private Event resultEvent;
  private MuleException exception;

  public ProcessingMuleEventWork(Processor messageProcessor, Event muleEvent, MuleContext muleContext,
                                 FlowConstruct flowConstruct) {
    super(muleEvent);
    messageProcessorExecutionTemplate = createExecutionTemplate();
    messageProcessorExecutionTemplate.setMuleContext(muleContext);
    messageProcessorExecutionTemplate.setFlowConstruct(flowConstruct);
    this.messageProcessor = messageProcessor;
  }

  /**
   * Invokes {@link Processor#process(Event)} using {@link #messageProcessor and AbstractMuleEventWork#event}. if processing is
   * sucessful the result is stored in {@link #resultEvent} or if it throws exception, it will be stored in {@link #exception}
   * Storing the result/exception allows {@link #getResult(long, TimeUnit)} to return the values
   */
  @Override
  protected void doRun() {
    try {
      this.resultEvent = messageProcessorExecutionTemplate.execute(messageProcessor, event);
    } catch (MuleException e) {
      this.exception = e;
    } finally {
      this.latch.countDown();
    }
  }

  /**
   * This method is useful for having a thread other than the one executing this work waiting for the result being available. This
   * is similar to {@link Future#get(long, TimeUnit)} but with some significant differences, mainly around the type of exceptions
   * to be thrown and the fact that this work cannot cancel itself
   * 
   * @param timeout time to wait before throwing {@link ResponseTimeoutException}
   * @param timeUnit the unit for the timeout
   * @return a {@link Event} once that {@link #doRun()} finished successfuly
   * @throws InterruptedException if the calling thread is interrupted
   * @throws ResponseTimeoutException if the calling thread waiting time has exceeded the timeout and {@link #doRun()} hasn't yet
   *         finished
   * @throws MuleException if {@link #doRun()} finished with exception. In that case, the value captured in {@link #exception} is
   *         thrown
   */
  public Event getResult(long timeout, TimeUnit timeUnit)
      throws InterruptedException, ResponseTimeoutException, MuleException {
    if (this.latch.await(timeout, timeUnit)) {
      if (this.exception != null) {
        throw this.exception;
      }

      return this.resultEvent;
    } else {
      throw new ResponseTimeoutException(I18nMessageFactory.createStaticMessage("Processing did not completed in time"),
                                         this.messageProcessor);
    }
  }
}
