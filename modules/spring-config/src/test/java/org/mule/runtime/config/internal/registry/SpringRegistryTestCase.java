/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mockito.ArgumentMatchers.any;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION;

import static java.util.Collections.singletonMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import org.junit.After;
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
  public void registryIsReadOnlyWhileStopping() throws MuleException {
    LifecycleInterceptor lifecycleInterceptor = mock(LifecycleInterceptor.class);
    when(lifecycleInterceptor.beforePhaseExecution(any(), any())).thenReturn(true);
    final BeanWithLifeCycle beanWithLifeCycle = new BeanWithLifeCycle(executor, Stoppable.PHASE_NAME);
    SpringRegistry registry =
        buildLifeCycleTesteableSpringRegistry(lifecycleInterceptor, singletonMap("beanWithLifeCycle", beanWithLifeCycle));
    beanWithLifeCycle.setRegistryUnderTest(registry);
    // Previous phases are run for consistency of the Registry and the LifeCycleManager
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

  @Test
  public void registryIsReadOnlyWhileDisposing() throws MuleException {
    LifecycleInterceptor lifecycleInterceptor = mock(LifecycleInterceptor.class);
    when(lifecycleInterceptor.beforePhaseExecution(any(), any())).thenReturn(true);
    final BeanWithLifeCycle beanWithLifeCycle = new BeanWithLifeCycle(executor, Disposable.PHASE_NAME);
    SpringRegistry registry =
        buildLifeCycleTesteableSpringRegistry(lifecycleInterceptor, singletonMap("beanWithLifeCycle", beanWithLifeCycle));
    beanWithLifeCycle.setRegistryUnderTest(registry);
    // Previous phases are run for consistency of the Registry and the LifeCycleManager
    registry.fireLifecycle(Initialisable.PHASE_NAME);
    registry.fireLifecycle(Startable.PHASE_NAME);
    registry.fireLifecycle(Stoppable.PHASE_NAME);
    executor.submit(() -> {
      try {
        registry.fireLifecycle(Disposable.PHASE_NAME);
      } catch (LifecycleException e) {
        throw new RuntimeException(e);
      }
    });
    probe(registry::hasPendingRegistrations, () -> "Registry should be read read only while disposing");
  }

  /**
   * This builds a SpringRegistry whose lifecycle can be invoked and tested.
   * 
   * @param lifecycleInterceptor     The {@link LifecycleInterceptor} to be used.
   * @param singletonBeansToRegister key/value map of singleton beans that will be already registered in the returned
   *                                 {@link SpringRegistry}
   * @return {@link SpringRegistry} that can be tested via invoking its lifecycle phases.
   */
  private SpringRegistry buildLifeCycleTesteableSpringRegistry(LifecycleInterceptor lifecycleInterceptor,
                                                               Map<String, ?> singletonBeansToRegister) {
    // This makes the bean look like a registered bean (The ApplicationContext is mocked).
    singletonBeansToRegister.forEach((key, value) -> {
      when(appContext.getBean(key)).thenReturn(value);
      when(appContext.isSingleton(key)).thenReturn(true);
      // No dependencies for any of the beans.
      when(beanFactory.getDependenciesForBean(key)).thenReturn(new String[0]);
    });
    // The registry is created and will return all the previously registered beans as lifecycle objects.
    return new SpringRegistry(appContext, appContext,
                              null, mock(ConfigurationDependencyResolver.class),
                              lifecycleInterceptor) {

      @Override
      <T> Map<String, T> lookupEntriesForLifecycle(Class<T> type) {
        return (Map<String, T>) singletonBeansToRegister;
      }

      @Override
      protected <T> Map<String, T> lookupEntriesForLifecycleIncludingAncestors(Class<T> type) {
        return (Map<String, T>) singletonBeansToRegister;
      }

    };
  }

  public static class BeanWithLifeCycle implements Stoppable, Disposable {

    private SpringRegistry registryUnderTest;
    private final ExecutorService registrationExecutor;
    private final String phaseUnderTest;

    public BeanWithLifeCycle(ExecutorService registrationExecutor, String phaseUnderTest) {
      this.registrationExecutor = registrationExecutor;
      this.phaseUnderTest = phaseUnderTest;
    }

    @Override
    public void stop() {
      if (phaseUnderTest.equals(Stoppable.PHASE_NAME)) {
        addObjectToRegistryAndFreezeCurrentThread("stopObject", new Object());
      }
    }

    @Override
    public void dispose() {
      if (phaseUnderTest.equals(Disposable.PHASE_NAME)) {
        addObjectToRegistryAndFreezeCurrentThread("disposeObject", new Object());
      }
    }

    private void addObjectToRegistryAndFreezeCurrentThread(String registryKey, Object objectToAdd) {
      // This registration will be put on hold buy the registry if it is being stopped / disposed
      registrationExecutor.submit(() -> {
        try {
          registryUnderTest.registerObject(registryKey, objectToAdd);
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

    public void setRegistryUnderTest(SpringRegistry registryUnderTest) {
      this.registryUnderTest = registryUnderTest;
    }
  }

}
