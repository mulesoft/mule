/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.requestreply;

import static java.util.Collections.singletonMap;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.store.SimpleMemoryObjectStore;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.internal.routing.correlation.EventCorrelatorTestCase;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.routing.ResponseTimeoutException;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import java.beans.ExceptionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRequestReplyRequesterTestCase extends AbstractMuleContextTestCase implements ExceptionListener {

  private static final Logger LOGGER = getLogger(EventCorrelatorTestCase.class);

  private Scheduler scheduler;

  private TestAsyncRequestReplyRequester asyncReplyMP;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put(OBJECT_STORE_MANAGER, new MuleObjectStoreManager());
    objects.put(REGISTRY_KEY, componentLocator);

    return objects;
  }


  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    scheduler = muleContext.getSchedulerService().cpuIntensiveScheduler();
    createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
  }

  @Override
  protected void doTearDown() throws Exception {
    scheduler.stop();
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

    CoreEvent resultEvent = asyncReplyMP.process(testEvent());

    // Can't assert same because we copy event when we receive async reply
    assertEquals(((PrivilegedEvent) testEvent()).getMessageAsString(muleContext),
                 ((PrivilegedEvent) resultEvent).getMessageAsString(muleContext));
  }

  @Test
  public void testSingleEventNoTimeoutAsync() throws Exception {
    asyncReplyMP = new TestAsyncRequestReplyRequester(muleContext);
    SensingNullMessageProcessor target = getSensingNullMessageProcessor();
    AsyncDelegateMessageProcessor asyncMP = createAsyncMessageProcessor(target);
    initialiseIfNeeded(asyncMP, true, muleContext);
    asyncMP.start();
    asyncReplyMP.setListener(asyncMP);
    asyncReplyMP.setReplySource(target.getMessageSource());
    asyncReplyMP.setMuleContext(muleContext);

    CoreEvent resultEvent = asyncReplyMP.process(testEvent());

    // Can't assert same because we copy event for async and also on async reply currently
    assertEquals(((PrivilegedEvent) testEvent()).getMessageAsString(muleContext),
                 ((PrivilegedEvent) resultEvent).getMessageAsString(muleContext));
  }

  @Test
  public void testSingleEventTimeout() throws Exception {
    asyncReplyMP = new TestAsyncRequestReplyRequester(muleContext);
    asyncReplyMP.setTimeout(1);
    SensingNullMessageProcessor target = getSensingNullMessageProcessor();
    target.setWaitTime(30000);
    AsyncDelegateMessageProcessor asyncMP = createAsyncMessageProcessor(target);
    initialiseIfNeeded(asyncMP, true, muleContext);
    asyncMP.start();
    asyncReplyMP.setListener(asyncMP);
    asyncReplyMP.setReplySource(target.getMessageSource());
    asyncReplyMP.setMuleContext(muleContext);

    CoreEvent event = eventBuilder(muleContext).message(of(TEST_MESSAGE)).build();

    try {
      asyncReplyMP.process(event);
      fail("ResponseTimeoutException expected");
    } catch (Exception e) {
      assertThat(e, instanceOf(ResponseTimeoutException.class));
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
          CoreEvent resultEvent = asyncReplyMP.process(testEvent());

          // Can't assert same because we copy event for async currently
          assertEquals(((PrivilegedEvent) testEvent()).getMessageAsString(muleContext),
                       ((PrivilegedEvent) resultEvent).getMessageAsString(muleContext));
          count.incrementAndGet();
          LOGGER.debug("Finished " + count.get());
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

  @Test
  public void testResponseEventsCleanedUp() throws Exception {
    RelaxedAsyncReplyMP mp = new RelaxedAsyncReplyMP(muleContext);

    try {
      CoreEvent event =
          eventBuilder(muleContext).message(of("message1")).groupCorrelation(Optional.of(GroupCorrelation.of(0, 3))).build();

      SensingNullMessageProcessor listener = getSensingNullMessageProcessor();
      mp.setListener(listener);
      mp.setReplySource(listener.getMessageSource());

      mp.process(event);

      Map<String, PrivilegedEvent> responseEvents = mp.getResponseEvents();
      assertThat(responseEvents.entrySet(), empty());
    } finally {
      mp.stop();
    }
  }

  private AsyncDelegateMessageProcessor asyncMP;

  @After
  public void after() throws MuleException {
    stopIfNeeded(asyncMP);
    disposeIfNeeded(asyncMP, LOGGER);
  }

  protected AsyncDelegateMessageProcessor createAsyncMessageProcessor(SensingNullMessageProcessor target)
      throws InitialisationException {
    asyncMP = new AsyncDelegateMessageProcessor(newChain(Optional.empty(), target));
    asyncMP.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(asyncMP, true, muleContext);
    return asyncMP;
  }

  @Override
  public void exceptionThrown(Exception e) {
    e.printStackTrace();
    fail(e.getMessage());
  }

  class TestAsyncRequestReplyRequester extends AbstractAsyncRequestReplyRequester {

    TestAsyncRequestReplyRequester(MuleContext muleContext) throws MuleException {
      setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
      setMuleContext(muleContext);
      initialise();
      start();
    }
  }

  /**
   * This class opens up the access to responseEvents map for testing
   */
  private static final class RelaxedAsyncReplyMP extends AbstractAsyncRequestReplyRequester {

    private RelaxedAsyncReplyMP(MuleContext muleContext) throws MuleException {
      store = new SimpleMemoryObjectStore<>();
      name = "asyncReply";
      setMuleContext(muleContext);
      start();
    }

    public Map<String, PrivilegedEvent> getResponseEvents() {
      return responseEvents;
    }
  }
}
