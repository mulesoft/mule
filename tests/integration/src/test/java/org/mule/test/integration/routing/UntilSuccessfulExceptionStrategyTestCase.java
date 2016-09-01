/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.tck.MuleTestUtils.createErrorMock;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

public class UntilSuccessfulExceptionStrategyTestCase extends AbstractIntegrationTestCase {

  private static final int TIMEOUT = 10;
  private static Latch latch;

  @Before
  public void setUp() {
    latch = new Latch();
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/until-successful-exception-strategy-config.xml";
  }

  @Test
  public void usingSimpleSetUp() throws Exception {
    testHandlingOfFailures("simpleTest");
  }

  @Test
  public void usingSimpleSetUpAndNoRetries() throws Exception {
    testHandlingOfFailures("noRetriesSimpleTest");
  }

  @Test
  public void usingSplitterAggregator() throws Exception {
    testHandlingOfFailures("withSplitterAggregatorTest");
  }

  @Test
  public void usingSplitterAggregatorAndNoRetries() throws Exception {
    testHandlingOfFailures("noRetriesSplitterAggregatorTest");
  }

  private void testHandlingOfFailures(String entryPoint) throws Exception {
    MuleEvent event = flowRunner(entryPoint).withPayload(getTestMuleMessage()).run();
    MuleMessage response = event.getMessage();
    assertThat(event.getError(), is(nullValue()));
    assertThat(getPayloadAsString(response), is("ok"));
  }

  public static class LockProcessor implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      try {
        if (!latch.await(TIMEOUT, TimeUnit.SECONDS)) {
          RuntimeException exception = new RuntimeException();
          event = MuleEvent.builder(event)
              .message(MuleMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build())
              .error(createErrorMock(exception)).build();
        }
      } catch (InterruptedException e) {
        // do nothing
      }
      return event;
    }
  }

  public static class UnlockProcessor implements MessageProcessor {

    AtomicInteger count;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      if (count.decrementAndGet() == 0) {
        latch.release();
      }
      return event;
    }

  }

  public static class WaitTwiceBeforeUnlockProcessor extends UnlockProcessor {

    public WaitTwiceBeforeUnlockProcessor() {
      count = new AtomicInteger(2);
    }

  }

  public static class WaitOnceBeforeUnlockProcessor extends UnlockProcessor {

    public WaitOnceBeforeUnlockProcessor() {
      count = new AtomicInteger(1);
    }
  }
}
