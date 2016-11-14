/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import static java.util.Arrays.asList;
import static org.mule.tck.MuleTestUtils.processAsStreamAndBlock;
import static reactor.core.Exceptions.unwrap;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;

/**
 * Abstract base test case extending {@link AbstractMuleContextTestCase} to be used when a {@link Processor} or
 * {@link org.mule.runtime.core.construct.Flow} that implements both {@link Processor#process(Event)} and
 * {@link Processor#apply(Publisher)} needs paramatized tests so that both approaches are tested with the same test method. Test
 * cases that extend this abstract class should use (@link {@link #process(Processor, Event)} to invoke {@link Processor}'s as
 * part of the test, rather than invoking them directly.
 */
@RunWith(Parameterized.class)
public abstract class AbstractReactiveProcessorTestCase extends AbstractMuleContextTestCase {

  protected Scheduler scheduler;

  private boolean reactive;

  public AbstractReactiveProcessorTestCase(boolean reactive) {
    this.reactive = reactive;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return asList(new Object[][] {{false}, {true}});
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    scheduler = muleContext.getSchedulerService().computationScheduler();
  }

  @Override
  protected void doTearDown() throws Exception {
    scheduler.shutdownNow();
    super.doTearDown();
  }

  @Override
  protected Event process(Processor processor, Event event) throws Exception {
    try {
      if (reactive) {
        return processAsStreamAndBlock(event, processor);
      } else {
        return processor.process(event);
      }
    } catch (MessagingException msgException) {
      // unwrap MessagingException to ensure same exception is thrown by blocking and non-blocking processing
      throw (msgException.getCause() instanceof Exception) ? (Exception) msgException.getCause()
          : new RuntimeException(msgException.getCause());
    }
  }

  /*
   * Do not unwrap MessagingException thrown by use of apply() for compatability with flow.process()
   */
  protected Event processFlow(Flow flow, Event event) throws Exception {
    if (reactive) {
      try {
        return just(event)
            .transform(flow)
            .subscribe()
            .blockMillis(RECEIVE_TIMEOUT);
      } catch (Throwable exception) {
        throw (Exception) unwrap(exception);
      }
    } else {
      return flow.process(event);
    }
  }

  protected boolean isReactive() {
    return reactive;
  }
}
