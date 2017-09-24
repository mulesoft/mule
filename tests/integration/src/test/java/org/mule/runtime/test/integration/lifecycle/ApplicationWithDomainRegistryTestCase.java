/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.lifecycle;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObject;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObjects;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.lookupObjectsForLifecycle;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.DomainObjectRegistrationStory.OBJECT_REGISTRATION;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.functional.junit4.DomainContextBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.allure.AllureConstants;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REGISTRY)
public class ApplicationWithDomainRegistryTestCase extends AbstractMuleTestCase {

  public static final String BEAN_KEY = "someBean";
  public static final String ANOTHER_BEAN_KEY = "someOtherBean";

  private MuleContext domainContext;
  private MuleContext applicationContext;

  @Before
  public void createContexts() throws Exception {
    domainContext = new DomainContextBuilder().build();
    applicationContext = new ApplicationContextBuilder().setDomainContext(domainContext).build();
  }

  @Story(OBJECT_REGISTRATION)
  @Test
  public void lookupByTypeSearchInParentAlso() throws Exception {
    registerObject(domainContext, BEAN_KEY, BEAN_KEY);
    registerObject(applicationContext, ANOTHER_BEAN_KEY, ANOTHER_BEAN_KEY);
    Collection<String> values = lookupObjects(applicationContext, String.class);
    assertThat(values, hasSize(2));
  }

  @Story(OBJECT_REGISTRATION)
  @Test
  public void lookupByIdReturnsApplicationContextBeanEvenIfSameBeanIsInDomain() throws Exception {
    registerObject(applicationContext, BEAN_KEY, BEAN_KEY);
    registerObject(domainContext, BEAN_KEY, new Integer(10));
    assertThat(lookupObject(applicationContext, BEAN_KEY), is(instanceOf(String.class)));
  }

  @Story(OBJECT_REGISTRATION)
  @Test
  public void lookupByLifecycleReturnsApplicationContextBeanOnly() throws Exception {
    registerObject(domainContext, BEAN_KEY, BEAN_KEY);
    registerObject(applicationContext, ANOTHER_BEAN_KEY, ANOTHER_BEAN_KEY);
    assertThat(lookupObjectsForLifecycle(applicationContext, String.class).size(), is(1));
  }

  @Story(AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION)
  @Test
  public void lookupByIdReturnsParentApplicationContextBean() throws Exception {
    Object value = new Object();
    registerObject(applicationContext, BEAN_KEY, value);
    assertThat(lookupObject(applicationContext, BEAN_KEY), is(value));
  }

  @Story(AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION)
  @Test
  public void registerObjectAsynchronously() throws MuleException, InterruptedException, ExecutionException {
    final Startable asyncStartableBean = mock(Startable.class, withSettings().extraInterfaces(Disposable.class));

    final Latch startingLatch = new Latch();
    final Latch disposingLatch = new Latch();

    doAnswer(invocation -> {
      startingLatch.countDown();
      synchronized (applicationContext) {
        disposingLatch.await();
      }

      verify((Disposable) asyncStartableBean, never()).dispose();
      return null;
    }).when(asyncStartableBean).start();

    ((Disposable) doAnswer(invocation -> null).when(asyncStartableBean)).dispose();

    final ExecutorService threadPool = newCachedThreadPool();

    try {
      final Future<?> submittedStop = threadPool.submit(() -> {
        try {
          registerObject(applicationContext, BEAN_KEY, asyncStartableBean);
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
        synchronized (applicationContext) {
          applicationContext.dispose();
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
