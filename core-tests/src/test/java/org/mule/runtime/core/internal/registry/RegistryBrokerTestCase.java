/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.runtime.core.api.util.UUID.getUUID;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.lifecycle.MuleLifecycleInterceptor;
import org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Kiwi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RegistryBrokerTestCase extends AbstractMuleContextTestCase {

  private String tracker;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    tracker = new String();
  }

  @Override
  protected boolean isStartContext() {
    return false;
  }

  @Test
  public void testCrossRegistryLifecycleOrder() throws MuleException {

    TransientRegistry reg1 = new TransientRegistry(getUUID(), muleContext, new MuleLifecycleInterceptor());
    reg1.initialise();
    TransientRegistry reg2 = new TransientRegistry(getUUID(), muleContext, new MuleLifecycleInterceptor());
    reg2.initialise();

    reg1.registerObject("flow", new LifecycleTrackerFlow("flow", muleContext));
    reg2.registerObject("flow2", new LifecycleTrackerFlow("flow2", muleContext));

    ((MuleContextWithRegistries) muleContext).addRegistry(reg1);
    ((MuleContextWithRegistries) muleContext).addRegistry(reg2);

    muleContext.start();

    // Both connectors are started before either flow
    assertEquals("flow2-start flow-start ", tracker.toString());

    tracker = new String();
    muleContext.stop();

    // Both services are stopped before either connector
    assertEquals("flow2-stop flow-stop ", tracker);
  }

  class LifecycleTrackerFlow extends DefaultFlowBuilder.DefaultFlow {

    public LifecycleTrackerFlow(String name, MuleContext muleContext) {
      super(name, muleContext, null, emptyList(), empty(), empty(), INITIAL_STATE_STARTED, DEFAULT_MAX_CONCURRENCY,
            new DefaultFlowsSummaryStatistics(true), null,
            new ComponentInitialStateManager() {

              @Override
              public boolean mustStartMessageSource(Component component) {
                return true;
              }
            });
    }

    @Override
    protected void doStart() throws MuleException {
      super.doStart();
      tracker += getName() + "-start ";
    }

    @Override
    protected void doStop() throws MuleException {
      super.doStop();
      tracker += getName() + "-stop ";
    }
  }

  @Test
  public void testConcurrentRegistryAddRemove() throws Exception {
    MuleLifecycleInterceptor lifecycleInterceptor = new MuleLifecycleInterceptor();
    final RegistryBroker broker =
        new DefaultRegistryBroker(muleContext, lifecycleInterceptor);

    final int N = 50;
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch end = new CountDownLatch(N);
    final AtomicInteger errors = new AtomicInteger(0);
    for (int i = 0; i < N; i++) {
      new Thread(() -> {
        try {
          start.await();
          broker.addRegistry(new TransientRegistry(getUUID(), muleContext, lifecycleInterceptor));
          broker.lookupByType(Object.class);
        } catch (Exception e) {
          errors.incrementAndGet();
        } finally {
          end.countDown();
        }
      }, "thread-eval-" + i).start();
    }
    start.countDown();
    end.await();
    if (errors.get() > 0) {
      fail();
    }
  }

  @Test
  public void registerWhenNoRegistriesManuallyAddedYet() throws Exception {
    final String KEY1 = "apple";
    final Object VALUE1 = new Apple();
    final String KEY2 = "Kiwi";
    final Object VALUE2 = new Kiwi();

    MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();
    registry.registerObject(KEY1, VALUE1);
    registry.registerObject(KEY2, VALUE2);

    assertThat(registry.get(KEY1), is(VALUE1));
    assertThat(registry.get(KEY2), is(VALUE2));

    assertThat(registry.lookupObject(Apple.class), is(VALUE1));
    assertThat(registry.lookupObject(Kiwi.class), is(VALUE2));
  }

}
