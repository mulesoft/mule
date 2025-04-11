/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION;

import static java.util.Collections.singletonMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(REGISTRY)
@Story(OBJECT_REGISTRATION)
public class SpringRegistryTestCase extends AbstractMuleTestCase {

  private ConfigurableApplicationContext appContext;

  private DefaultListableBeanFactory beanFactory;

  private ExecutorService executor;

  @Before
  public void setUp() {
    appContext = mock(ConfigurableApplicationContext.class);
    beanFactory = new DefaultListableBeanFactory();
    when(appContext.getBeanFactory()).thenReturn(beanFactory);
    executor = Executors.newFixedThreadPool(1);
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

    beanFactory.registerBeanDefinition("key", new GenericBeanDefinition());
    when(appContext.getBean("key")).thenThrow(BeanCreationException.class);

    assertThat(registry.unregisterObject("key"), is(nullValue()));
    assertThat(assertThrows(NoSuchBeanDefinitionException.class, () -> beanFactory.getBeanDefinition("key")),
               hasMessage("No bean named 'key' available"));
    assertThat(beanFactory.getSingleton("key"), nullValue());
  }

  @Test
  @Issue("W-14722908")
  public void registryIsReadOnlyWhileStopping() throws LifecycleException {
    LifecycleInterceptor lifecycleInterceptor = mock(LifecycleInterceptor.class);
    when(lifecycleInterceptor.beforePhaseExecution(any(), any())).thenReturn(true);
    final BeanWithLifeCycle beanWithLifeCycle = new BeanWithLifeCycle(executor, Stoppable.PHASE_NAME);
    SpringRegistry registry =
        buildLifeCycleTesteableSpringRegistry(lifecycleInterceptor, singletonMap("beanWithLifeCycle", beanWithLifeCycle));
    beanWithLifeCycle.setRegistryUnderTest(registry);
    // Previous phases are run for consistency of the Registry and the LifeCycleManager
    registry.fireLifecycle(Initialisable.PHASE_NAME);
    registry.fireLifecycle(Startable.PHASE_NAME);
    // The stop phase will trigger a registry update intent via BeanWithLifeCycle.stop()
    registry.fireLifecycle(Stoppable.PHASE_NAME);
    assertThat(beanWithLifeCycle.getRegistrationException().isPresent(), is(true));
    assertThat(beanWithLifeCycle.getRegistrationException().get().getMessage(),
               containsString("Could not add entry with key 'stopObject': Registry has been stopped."));
  }

  @Test
  @Issue("W-14722908")
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
    // The stop phase will trigger a registry update intent via BeanWithLifeCycle.dispose()
    registry.fireLifecycle(Disposable.PHASE_NAME);
    assertThat(beanWithLifeCycle.getRegistrationException().isPresent(), is(true));
    assertThat(beanWithLifeCycle.getRegistrationException().get().getMessage(),
               containsString("Could not add entry with key 'disposeObject': Registry has been stopped."));
  }

  /**
   * This builds a SpringRegistry whose lifecycle can be invoked and tested.
   *
   * @param lifecycleInterceptor     The {@link LifecycleInterceptor} to be used.
   * @param singletonBeansToRegister key/value map of singleton beans that will be already registered in the returned
   *                                 {@link SpringRegistry}
   * @return A {@link SpringRegistry} that can be tested via invoking its lifecycle phases.
   */
  private SpringRegistry buildLifeCycleTesteableSpringRegistry(LifecycleInterceptor lifecycleInterceptor,
                                                               Map<String, ?> singletonBeansToRegister) {
    // This makes the beans look like a registered Spring beans (Spring's ApplicationContext is mocked).
    singletonBeansToRegister.forEach((key, value) -> {
      when(appContext.getBean(key)).thenReturn(value);
      when(appContext.isSingleton(key)).thenReturn(true);

      // No dependencies for any of the registered beans.
      beanFactory.registerBeanDefinition(key, new GenericBeanDefinition());
    });
    return new SpringRegistry(appContext, appContext,
                              null, mock(ConfigurationDependencyResolver.class),
                              lifecycleInterceptor) {

      @Override
      <T> Map<String, T> lookupEntriesForLifecycle(Class<T> type) {
        // The will return all the previously registered beans as lifecycle objects.
        return (Map<String, T>) singletonBeansToRegister;
      }

      @Override
      protected <T> Map<String, T> lookupEntriesForLifecycleIncludingAncestors(Class<T> type) {
        // The will return all the previously registered beans as lifecycle objects.
        return (Map<String, T>) singletonBeansToRegister;
      }
    };
  }

  public static class BeanWithLifeCycle implements Stoppable, Disposable {

    private SpringRegistry registryUnderTest;
    private final ExecutorService registrationExecutor;
    private final String phaseUnderTest;
    private volatile RegistrationException registrationException;

    /**
     * A bean that implements {@link Stoppable} and {@link Disposable} lifecycle phases and can try to register a bean as part of
     * each of them.
     *
     * @param registrationExecutor Executor that will be used to register the beans (registrations happen on a different thread).
     * @param phaseUnderTest       The lifecycle phase where the bean registration should be attempted.
     */
    public BeanWithLifeCycle(ExecutorService registrationExecutor, String phaseUnderTest) {
      this.registrationExecutor = registrationExecutor;
      this.phaseUnderTest = phaseUnderTest;
    }

    @Override
    public void stop() {
      if (phaseUnderTest.equals(Stoppable.PHASE_NAME)) {
        try {
          addObjectToRegistry("stopObject", new Object());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public void dispose() {
      if (phaseUnderTest.equals(Disposable.PHASE_NAME)) {
        try {
          addObjectToRegistry("disposeObject", new Object());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    private void addObjectToRegistry(String registryKey, Object objectToAdd) throws ExecutionException, InterruptedException {
      // This registration will fail if the registry is being shut down (starting to stop, stopped, starting to dispose or
      // disposed)
      registrationExecutor.submit(() -> {
        try {
          registryUnderTest.registerObject(registryKey, objectToAdd);
        } catch (RegistrationException e) {
          registrationException = e;
          // We throw the exception so the test can assert it.
          throw new RuntimeException(e);
        }
      }).get();
    }

    public void setRegistryUnderTest(SpringRegistry registryUnderTest) {
      this.registryUnderTest = registryUnderTest;
    }

    public Optional<RegistrationException> getRegistrationException() {
      return Optional.ofNullable(registrationException);
    }
  }

}
