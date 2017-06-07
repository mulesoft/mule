/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.tck.core.registry.AbstractRegistryTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

public class SpringRegistryTestCase extends AbstractRegistryTestCase {

  public static final String BEAN_KEY = "someBean";
  public static final String ANOTHER_BEAN_KEY = "someOtherBean";
  public static final String REGISTERY_ID = "someId";

  private StaticApplicationContext applicationContext;
  private StaticApplicationContext parentApplicationContext;
  private SpringRegistry springRegistry;

  @Override
  public Registry getRegistry() {
    return new SpringRegistry(new StaticApplicationContext(), null);
  }

  @Test
  public void lookupByTypeSearchInParentAlso() {
    createSpringRegistryWithParentContext();
    applicationContext.registerSingleton(BEAN_KEY, String.class);
    parentApplicationContext.registerSingleton(ANOTHER_BEAN_KEY, String.class);
    Collection<String> values = springRegistry.lookupObjects(String.class);
    assertThat(values.size(), is(2));
  }

  @Test
  public void lookupByIdReturnsApplicationContextBean() {
    createSpringRegistryWithParentContext();
    applicationContext.registerSingleton(BEAN_KEY, String.class);
    parentApplicationContext.registerSingleton(BEAN_KEY, Integer.class);
    assertThat(springRegistry.get(BEAN_KEY), is(instanceOf(String.class)));
  }

  @Test
  public void lookupByIdReturnsParentApplicationContextBean() {
    createSpringRegistryWithParentContext();
    parentApplicationContext.registerSingleton(BEAN_KEY, Object.class);
    assertThat(springRegistry.get(BEAN_KEY), Is.is(Object.class));
  }

  @Test
  public void lookupByLifecycleReturnsApplicationContextBeanOnly() {
    createSpringRegistryWithParentContext();
    applicationContext.registerSingleton(BEAN_KEY, String.class);
    parentApplicationContext.registerSingleton(ANOTHER_BEAN_KEY, String.class);
    assertThat(springRegistry.lookupObjectsForLifecycle(String.class).size(), is(1));
  }

  private void createSpringRegistryWithParentContext() {
    applicationContext = new StaticApplicationContext();
    parentApplicationContext = new StaticApplicationContext();
    springRegistry = new SpringRegistry(REGISTERY_ID, applicationContext, parentApplicationContext, null);
  }

  @Test
  public void registerObjectAsynchronously() throws MuleException, InterruptedException, ExecutionException {
    final MuleContext muleContext = mock(MuleContext.class);
    springRegistry = new SpringRegistry(new StaticApplicationContext(), muleContext);
    springRegistry.initialise();
    springRegistry.getLifecycleManager().fireLifecycle(Startable.PHASE_NAME);

    final Startable asyncStartableBean = mock(Startable.class, withSettings().extraInterfaces(Disposable.class));

    final Latch startingLatch = new Latch();
    final Latch disposingLatch = new Latch();

    doAnswer(invocation -> {
      startingLatch.countDown();
      synchronized (muleContext) {
        disposingLatch.await();
      }

      verify((Disposable) asyncStartableBean, never()).dispose();
      return null;
    }).when(asyncStartableBean).start();

    ((Disposable) doAnswer(invocation -> {
      return null;
    }).when(asyncStartableBean)).dispose();

    final ExecutorService threadPool = newCachedThreadPool();

    try {
      final Future<?> submittedStop = threadPool.submit(() -> {
        try {
          springRegistry.registerObject(BEAN_KEY, asyncStartableBean);
        } catch (RegistrationException e) {
          throw new MuleRuntimeException(e);
        }
      });
      final Future<?> submittedRegistryDispose = threadPool.submit(() -> {
        try {
          startingLatch.await();
        } catch (InterruptedException e) {
          throw new MuleRuntimeException(e);
        }
        disposingLatch.countDown();
        synchronized (muleContext) {
          springRegistry.dispose();
        }
      });

      submittedRegistryDispose.get();
      submittedStop.get();
    } finally {
      threadPool.shutdownNow();
    }

    new PollingProber().check(new JUnitLambdaProbe(() -> {
      try {
        verify(asyncStartableBean).start();
        verify((Disposable) asyncStartableBean).dispose();
        return true;
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }));

  }
}
