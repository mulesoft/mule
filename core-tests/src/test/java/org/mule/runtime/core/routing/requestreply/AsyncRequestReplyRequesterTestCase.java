/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.requestreply;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.ResponseTimeoutException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.store.MuleObjectStoreManager;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.beans.ExceptionListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class AsyncRequestReplyRequesterTestCase extends AbstractMuleContextTestCase implements ExceptionListener {

  private Scheduler scheduler;

  TestAsyncRequestReplyRequester asyncReplyMP;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    muleContext.getRegistry().registerObject(OBJECT_STORE_MANAGER, new MuleObjectStoreManager());
    scheduler = muleContext.getSchedulerService().cpuIntensiveScheduler();
  }

  @Override
  protected void doTearDown() throws Exception {
    scheduler.shutdownNow();
    if (asyncReplyMP != null) {
      asyncReplyMP.stop();
      asyncReplyMP.dispose();
    }
    super.doTearDown();
  }

  @Test
  public void testSingleEventNoTimeout() throws Exception {
    asyncReplyMP = new TestAsyncRequestReplyRequester(muleContext);
    SensingNullMessageProcessor target = getSensingNullMessageProcessor();

    asyncReplyMP.setListener(target);
    asyncReplyMP.setReplySource(target.getMessageSource());
    asyncReplyMP.setMuleContext(muleContext);

    Event resultEvent = asyncReplyMP.process(testEvent());

    // Can't assert same because we copy event when we receive async reply
    assertEquals(testEvent().getMessageAsString(muleContext), resultEvent.getMessageAsString(muleContext));
  }

  @Test
  public void testSingleEventNoTimeoutAsync() throws Exception {
    asyncReplyMP = new TestAsyncRequestReplyRequester(muleContext);
    SensingNullMessageProcessor target = getSensingNullMessageProcessor();
    AsyncDelegateMessageProcessor asyncMP = createAsyncMessageProcessor(target);
    asyncMP.setFlowConstruct(new Flow("flowName", muleContext));
    asyncMP.initialise();
    asyncMP.start();
    asyncReplyMP.setListener(asyncMP);
    asyncReplyMP.setReplySource(target.getMessageSource());
    asyncReplyMP.setMuleContext(muleContext);

    Event resultEvent = asyncReplyMP.process(testEvent());

    // Can't assert same because we copy event for async and also on async reply currently
    assertEquals(testEvent().getMessageAsString(muleContext), resultEvent.getMessageAsString(muleContext));
  }

  @Test
  public void testSingleEventTimeout() throws Exception {
    asyncReplyMP = new TestAsyncRequestReplyRequester(muleContext);
    asyncReplyMP.setTimeout(1);
    SensingNullMessageProcessor target = getSensingNullMessageProcessor();
    target.setWaitTime(30000);
    AsyncDelegateMessageProcessor asyncMP = createAsyncMessageProcessor(target);
    asyncMP.setFlowConstruct(new Flow("flowName", muleContext));
    asyncMP.initialise();
    asyncMP.start();
    asyncReplyMP.setListener(asyncMP);
    asyncReplyMP.setReplySource(target.getMessageSource());
    asyncReplyMP.setMuleContext(muleContext);

    Event event = eventBuilder().message(InternalMessage.of(TEST_MESSAGE)).build();

    try {
      asyncReplyMP.process(event);
      fail("ResponseTimeoutException expected");
    } catch (Exception e) {
      assertEquals(ResponseTimeoutException.class, e.getClass());
    }
  }

  @Test
  @Ignore("See MULE-8830")
  public void returnsNullWhenInterruptedWhileWaitingForReply() throws Exception {
    final Latch fakeLatch = new Latch() {

      @Override
      public void await() throws InterruptedException {
        throw new InterruptedException();
      }
    };

    asyncReplyMP = new TestAsyncRequestReplyRequester(muleContext) {

      @Override
      protected Latch createEventLock() {
        return fakeLatch;
      }
    };

    final CountDownLatch processingLatch = new CountDownLatch(1);

    Processor target = mock(Processor.class);
    asyncReplyMP.setListener(target);

    MessageSource messageSource = mock(MessageSource.class);
    asyncReplyMP.setReplySource(messageSource);
    asyncReplyMP.setMuleContext(muleContext);

    final boolean[] exceptionThrown = new boolean[1];
    final Object[] responseEvent = new Object[1];

    Thread thread = new Thread(() -> {
      try {
        responseEvent[0] = asyncReplyMP.process(testEvent());
      } catch (MuleException e) {
        exceptionThrown[0] = true;
      } finally {
        processingLatch.countDown();
      }
    });

    thread.start();
    assertTrue(processingLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    assertFalse(exceptionThrown[0]);
    assertNull(responseEvent[0]);
  }

  @Test
  @Ignore("See MULE-8830")
  public void testMultiple() throws Exception {
    asyncReplyMP = new TestAsyncRequestReplyRequester(muleContext);
    SensingNullMessageProcessor target = getSensingNullMessageProcessor();
    target.setWaitTime(50);
    AsyncDelegateMessageProcessor asyncMP = createAsyncMessageProcessor(target);
    asyncMP.initialise();
    asyncReplyMP.setListener(asyncMP);
    asyncReplyMP.setReplySource(target.getMessageSource());

    final AtomicInteger count = new AtomicInteger();
    for (int i = 0; i < 500; i++) {
      scheduler.execute(() -> {
        try {
          Event resultEvent = asyncReplyMP.process(testEvent());

          // Can't assert same because we copy event for async currently
          assertEquals(testEvent().getMessageAsString(muleContext), resultEvent.getMessageAsString(muleContext));
          count.incrementAndGet();
          logger.debug("Finished " + count.get());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      assertThat(count.get(), greaterThanOrEqualTo(500));
      return true;
    }));
  }

  private AsyncDelegateMessageProcessor asyncMP;

  @After
  public void after() throws MuleException {
    stopIfNeeded(asyncMP);
    disposeIfNeeded(asyncMP, logger);
  }

  protected AsyncDelegateMessageProcessor createAsyncMessageProcessor(SensingNullMessageProcessor target) {
    asyncMP = new AsyncDelegateMessageProcessor(newChain(target));
    asyncMP.setMuleContext(muleContext);
    return asyncMP;
  }

  @Override
  public void exceptionThrown(Exception e) {
    e.printStackTrace();
    fail(e.getMessage());
  }

  class TestAsyncRequestReplyRequester extends AbstractAsyncRequestReplyRequester {

    TestAsyncRequestReplyRequester(MuleContext muleContext) throws MuleException {
      setMuleContext(muleContext);
      initialise();
      start();
    }
  }
}
