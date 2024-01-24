/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Feature(REGISTRY)
@Story(OBJECT_REGISTRATION)
public class SpringRegistryTestCase extends AbstractMuleTestCase {

  private ConfigurableApplicationContext appContext;

  private DefaultListableBeanFactory beanFactory;

  private ExecutorService executor;

  @Before
  public void setUp() {
    appContext = mock(ConfigurableApplicationContext.class);
    beanFactory = mock(DefaultListableBeanFactory.class);
    when(appContext.getBeanFactory()).thenReturn(beanFactory);
    executor = Executors.newFixedThreadPool(2);
  }

  @After
  public void tearDown() {

    executor.shutdownNow();
  }

  @Test
  @Issue("MULE-20042")
  public void unregisterBeanWhoseCreationFails() throws RegistrationException {
    SpringRegistry registry = new SpringRegistry(appContext, appContext,
                                                 mock(MuleContext.class), mock(ConfigurationDependencyResolver.class),
                                                 mock(LifecycleInterceptor.class));

    when(beanFactory.containsBeanDefinition("key")).thenReturn(true);
    when(appContext.getBean("key")).thenThrow(BeanCreationException.class);

    assertThat(registry.unregisterObject("key"), is(nullValue()));
    verify(beanFactory).removeBeanDefinition("key");
    verify(beanFactory).destroySingleton("key");
  }

  @Test
  public void registryIsReadOnlyWhileStoppingOrDisposing() throws MuleException {
    LifecycleInterceptor lifecycleInterceptor = mock(LifecycleInterceptor.class);
    when(lifecycleInterceptor.beforePhaseExecution(any(), any())).thenReturn(true);

    final BeanWithLifeCycle beanWithLifeCycle = new BeanWithLifeCycle(executor);

    SpringRegistry registry = new SpringRegistry(appContext, appContext,
                                                 null, mock(ConfigurationDependencyResolver.class),
                                                 lifecycleInterceptor) {

      final Map<String, Object> registeredBeans = singletonMap("beanWithLifeCycle", beanWithLifeCycle);

      @Override
      <T> Map<String, T> lookupEntriesForLifecycle(Class<T> type) {
        return (Map<String, T>) registeredBeans;
      }

      @Override
      protected <T> Map<String, T> lookupEntriesForLifecycleIncludingAncestors(Class<T> type) {
        return (Map<String, T>) registeredBeans;
      }

    };

    beanWithLifeCycle.setRegistry(registry);

    when(appContext.getBean("beanWithLifeCycle")).thenReturn(beanWithLifeCycle);
    when(beanFactory.getDependenciesForBean("beanWithLifeCycle")).thenReturn(new String[0]);
    registry.fireLifecycle(Initialisable.PHASE_NAME);
    registry.fireLifecycle(Startable.PHASE_NAME);
    executor.submit(() -> {
      try {
        registry.fireLifecycle(Stoppable.PHASE_NAME);
      } catch (LifecycleException e) {
        throw new RuntimeException(e);
      }
    });
    probe(registry::hasPendingRegistrations, () -> "Registry should be read read only while stopping");
  }

  public static class BeanWithLifeCycle implements Stoppable, Disposable {

    private SpringRegistry registry;
    private final ExecutorService registrationExecutor;

    public BeanWithLifeCycle(ExecutorService registrationExecutor) {
      this.registrationExecutor = registrationExecutor;
    }

    @Override
    public void stop() {
      // This registration should be put on hold buy the registry if it is being stopped / disposed.
      addObjectToRegistry("stopObject");
    }

    @Override
    public void dispose() {
      addObjectToRegistry("disposeObject");
    }

    private void addObjectToRegistry(String disposeObject) {
      // This registration should be put on hold buy the registry if it is being stopped / disposed
      registrationExecutor.submit(() -> {
        try {
          registry.registerObject(disposeObject, new Object());
        } catch (RegistrationException e) {
          // Log the exception instead of throwing it (the test assertion is the one that should fail)
          throw new RuntimeException(e);
        }
      });
      try {
        // We put this thread on a permanent wait so the current lifecycle phase does not change.
        new Latch().await();
      } catch (InterruptedException e) {
        // This is expected while shutting down the executor after the test.
      }
    }

    public void setRegistry(SpringRegistry registry) {
      this.registry = registry;
    }
  }

}
