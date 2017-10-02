/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.processor;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.interception.ProcessorInterceptorManager.PROCESSOR_INTERCEPTOR_MANAGER_REGISTRY_KEY;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ProcessorChainRouterStory.PROCESSOR_CHAIN_ROUTER;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.internal.interception.ProcessorInterceptorManager;
import org.mule.runtime.core.internal.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

// TODO MULE-13550 Improve CompositeProcessorChainRouter unit tests to cover scenario that was previously causing deadlock with flow-ref
@Feature(ROUTERS)
@Story(PROCESSOR_CHAIN_ROUTER)
public class CompositeProcessorChainRouterTestCase extends AbstractMuleContextTestCase {

  private CompositeProcessorChainRouter chainRouter;
  private AsyncDelegateMessageProcessor async;
  private Scheduler scheduler;

  @Before
  public void setup() throws MuleException {
    scheduler = muleContext.getSchedulerService().ioScheduler();
    createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    final Map<String, Object> objects = new HashMap<>();
    objects.put(REGISTRY_KEY, componentLocator);
    objects.put(PROCESSOR_INTERCEPTOR_MANAGER_REGISTRY_KEY, mock(ProcessorInterceptorManager.class));
    return objects;
  }

  @After
  public void tearDown() throws MuleException {
    chainRouter.stop();
    chainRouter.dispose();
    if (async != null) {
      async.stop();
      async.dispose();
    }
    scheduler.stop();
  }

  @Test
  @Description("Ensure that with simple chains both chains are executed consecutively with the result of the first chain being used for the second chain.")
  public void simpleChain() throws Exception {
    Message chain1Out = of(1);
    Message chain2Out = of(2);

    AtomicReference<Message> chain1In = new AtomicReference<>();
    AtomicReference<Message> chain2In = new AtomicReference<>();

    MessageProcessorChain chain1 = newChain(empty(), event -> {
      chain1In.set(event.getMessage());
      return builder(event).message(chain1Out).build();
    });
    MessageProcessorChain chain2 = newChain(empty(), event -> {
      chain2In.set(event.getMessage());
      return builder(event).message(chain2Out).build();
    });
    chainRouter = createCompositeProcessorChainRouter(chain1, chain2);

    Message result = chainRouter.execute(testEvent()).get().getMessage();

    assertThat(chain1In.get(), equalTo(testEvent().getMessage()));
    assertThat(chain2In.get(), equalTo(chain1Out));
    assertThat(result, equalTo(chain2Out));
  }

  @Test
  @Description("Ensure that when a child context is created as part of the execution of one of the composite chains then the chain does not complete and the next chains is not executed until the child context completes.")
  public void childContextChain() throws Exception {
    Latch latch = new Latch();
    AtomicReference<BaseEventContext> childEventContext = new AtomicReference<>();

    MessageProcessorChain chainUsingChildContext = newChain(empty(), event -> {
      latch.release();
      childEventContext.set(child((BaseEventContext) event.getContext(), empty()));
      return event;
    });
    chainRouter = createCompositeProcessorChainRouter(chainUsingChildContext, newChain(empty(), event -> event));

    // CompletableFuture is not returned immediately because simply invoking CompositeProcessorChainRouter there is no async
    // hand-off and so this blocks until child context completes.
    Future<CompletableFuture<Event>> future = scheduler.submit(() -> chainRouter.execute(testEvent()));

    latch.await();

    try {
      future.get(BLOCK_TIMEOUT, MILLISECONDS);
      fail("Timeout expected");
    } catch (TimeoutException te) {
    }

    childEventContext.get().success();

    assertThat(future.get(BLOCK_TIMEOUT, MILLISECONDS).get().getMessage(), equalTo(testEvent().getMessage()));
  }

  @Test
  @Description("Ensure that when an async scope is used as part of the execution of one of the composite chains then the chain does not complete and the next chains is not executed until the child context completes.")
  public void asyncDelegateChain() throws Exception {
    Latch latch = new Latch();
    Latch asyncLatch = new Latch();
    async = new AsyncDelegateMessageProcessor(newChain(empty(), event -> {
      try {
        asyncLatch.countDown();
        latch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return event;
    }));
    muleContext.getInjector().inject(async);
    async.setAnnotations(getAppleFlowComponentLocationAnnotations());

    chainRouter = createCompositeProcessorChainRouter(newChain(empty(), async), newChain(empty(), event -> event));
    chainRouter.start();

    // CompletableFuture is not returned immediately because simply invoking CompositeProcessorChainRouter there is no async
    // hand-off and so this blocks until child context completes.
    Future<CompletableFuture<Event>> future = scheduler.submit(() -> chainRouter.execute(testEvent()));

    asyncLatch.await();

    try {
      future.get(BLOCK_TIMEOUT, MILLISECONDS);
      fail("Timeout expected");
    } catch (TimeoutException te) {
    }

    latch.countDown();

    assertThat(future.get(BLOCK_TIMEOUT, MILLISECONDS).get().getMessage(), equalTo(testEvent().getMessage()));
  }

  private CompositeProcessorChainRouter createCompositeProcessorChainRouter(MessageProcessorChain chain1,
                                                                            MessageProcessorChain chain2)
      throws InitialisationException {
    CompositeProcessorChainRouter chainRouter = new CompositeProcessorChainRouter();
    chainRouter.setProcessorChains(asList(chain1, chain2));
    chainRouter.setMuleContext(muleContext);
    chainRouter.initialise();
    return chainRouter;
  }


}
