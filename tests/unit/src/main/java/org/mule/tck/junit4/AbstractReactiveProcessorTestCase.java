/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static java.util.Arrays.asList;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.tck.junit4.AbstractReactiveProcessorTestCase.Mode.BLOCKING;
import static org.mule.tck.junit4.AbstractReactiveProcessorTestCase.Mode.NON_BLOCKING;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * Abstract base test case extending {@link AbstractMuleContextTestCase} to be used when a {@link Processor} or {@link Flow} that
 * implements both {@link Processor#process(Event)} and {@link Processor#apply(Publisher)} needs paramatized tests so that both
 * approaches are tested with the same test method. Test cases that extend this abstract class should use (@link
 * {@link #process(Processor, Event)} to invoke {@link Processor}'s as part of the test, rather than invoking them directly.
 */
@RunWith(Parameterized.class)
public abstract class AbstractReactiveProcessorTestCase extends AbstractMuleContextTestCase {

  protected Scheduler scheduler;

  protected Mode mode;

  public AbstractReactiveProcessorTestCase(Mode mode) {
    this.mode = mode;
  }

  @Parameterized.Parameters
  public static Collection<Mode> modeParameters() {
    return asList(new Mode[] {BLOCKING, NON_BLOCKING});
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    scheduler = muleContext.getSchedulerService().cpuIntensiveScheduler();
  }

  @Override
  protected void doTearDown() throws Exception {
    scheduler.stop();
    super.doTearDown();
  }

  @Override
  protected Event process(Processor processor, Event event) throws Exception {
    return process(processor, event, true);
  }

  protected Event process(Processor processor, Event event, boolean unwrapMessagingException) throws Exception {
    setMuleContextIfNeeded(processor, muleContext);
    try {
      switch (mode) {
        case BLOCKING:
          return processor.process(event);
        case NON_BLOCKING:
          return processToApply(event, processor);
        default:
          return null;
      }
    } catch (Exception exception) {
      // Do not unwrap MessagingException thrown by use of apply() with Flow for compatibility with flow.process()
      if (unwrapMessagingException && (!(processor instanceof Flow) && exception instanceof MessagingException)) {
        throw messagingExceptionToException((MessagingException) exception);
      } else {
        throw exception;
      }
    }
  }

  private Exception messagingExceptionToException(MessagingException msgException) {
    // unwrap MessagingException to ensure same exception is thrown by blocking and non-blocking processing
    return (msgException.getCause() instanceof Exception) ? (Exception) msgException.getCause()
        : new RuntimeException(msgException.getCause());
  }

  public enum Mode {
    /**
     * Test using {@link Processor#process(Event)} blocking API.
     */
    BLOCKING,
    /**
     * Test using new reactive API by creating a {@link Mono} and blocking and waiting for completion (value, empty or error)
     */
    NON_BLOCKING,
  }
}
