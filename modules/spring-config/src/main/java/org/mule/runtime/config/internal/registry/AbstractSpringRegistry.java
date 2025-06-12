/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.util.InjectionUtils.getInjectionTarget;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.config.internal.context.ObjectProviderAwareBeanFactory;
import org.mule.runtime.config.internal.factories.ConstantFactoryBean;
import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.registry.AbstractRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public abstract class AbstractSpringRegistry extends AbstractRegistry implements SpringContextRegistry, Injector {

  public static final String REGISTRY_ID = "org.mule.Registry.Spring";

  /**
   * Key used to lookup Spring Application Context from SpringRegistry via Mule's Registry interface.
   */
  public static final String SPRING_APPLICATION_CONTEXT = "springApplicationContext";
  private static final String COULD_NOT_ADD_ENTRY_REGISTRY_HAS_BEEN_STOPPED =
      "Could not add entry with key '%s': Registry has been stopped.";

  private ApplicationContext applicationContext;
  private RegistrationDelegate registrationDelegate;
  private boolean readOnly;

  private final AtomicBoolean isStopped = new AtomicBoolean(false);

  // This is used to track the Spring context lifecycle since there is no way to confirm the
  // lifecycle phase from the application context
  protected AtomicBoolean springContextInitialised = new AtomicBoolean(false);

  // Registered objects before the spring registry has been initialised.
  protected final Map<String, BeanDefinition> registeredBeanDefinitionsBeforeInitialization = new HashMap<>();

  public AbstractSpringRegistry(ApplicationContext applicationContext,
                                MuleContext muleContext,
                                LifecycleInterceptor lifecycleInterceptor) {
    super(REGISTRY_ID, muleContext, lifecycleInterceptor);
    setApplicationContext(applicationContext);
  }

  protected void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    if (applicationContext instanceof ConfigurableApplicationContext) {
      readOnly = false;
      registrationDelegate = new ConfigurableRegistrationDelegate((ConfigurableApplicationContext) applicationContext);
    } else {
      readOnly = true;
      registrationDelegate = new ReadOnlyRegistrationDelegate();
    }
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  protected void addBeanFactoryPostProcessor(BeanDefinitionRegistryPostProcessor postProcessor) {
    ((AbstractApplicationContext) applicationContext).addBeanFactoryPostProcessor(postProcessor);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    if (!isReadOnly()) {
      ((ConfigurableApplicationContext) applicationContext).refresh();
    }
  }

  @Override
  public void doDispose() {
    // check we aren't trying to close a context which has never been started,
    // spring's appContext.isActive() isn't working for this case
    if (!springContextInitialised.get()) {
      return;
    }

    if (!isReadOnly() && ((ConfigurableApplicationContext) applicationContext).isActive()) {
      ((ConfigurableApplicationContext) applicationContext).close();
    }
    // release the circular implicit ref to MuleContext
    applicationContext = null;

    disposeContext();

    this.springContextInitialised.set(false);
  }

  protected void disposeContext() {
    // override
  }

  @Override
  protected LifecycleManager createLifecycleManager(LifecycleInterceptor lifecycleInterceptor) {
    return new SpringRegistryLifecycleManager(getRegistryId(), this, getMuleContext(), lifecycleInterceptor);
  }

  /**
   * {@inheritDoc}
   *
   * @return the result of invoking {@link #lookupObject(String, boolean)} with {@code true} as the second argument
   */
  @Override
  public <T> T lookupObject(String key) {
    try {
      return (T) lookupObject(key, true);
    } catch (NoSuchBeanDefinitionException e) {
      return null;
    }
  }

  /**
   * If looks for the bean registered under {@code key}. If the returned bean is a prototype and {@code applyLifecycle} is
   * {@code true}, then the completed lifecycle phases are applied to the returning bean
   *
   * @param key            the key of the object you're looking for
   * @param applyLifecycle if lifecycle should be applied to the returned object. Passing {@code true} doesn't guarantee that the
   *                       lifecycle is applied
   * @return object or {@code null} if not found
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object lookupObject(String key, boolean applyLifecycle) {
    if (StringUtils.isBlank(key)) {
      logger.warn(createStaticMessage("Detected a lookup attempt with an empty or null key").getMessage(),
                  new Throwable().fillInStackTrace());
      return null;
    }

    if (key.equals(SPRING_APPLICATION_CONTEXT) && applicationContext != null) {
      return applicationContext;
    } else {
      Object object;
      try {
        object = applicationContext.getBean(key);
      } catch (NoSuchBeanDefinitionException e) {
        if (logger.isTraceEnabled()) {
          logger.trace(e.getMessage(), e);
        } else {
          logger.debug(e.getMessage());
        }
        return null;
      }

      if (isNullBean(object)) {
        return null;
      }

      applyLifecycleIfPrototype(object, key, applyLifecycle);
      return object;
    }
  }

  protected final boolean isNullBean(Object bean) {
    return bean != null && "org.springframework.beans.factory.support.NullBean".equals(bean.getClass().getName());
  }

  protected final void applyLifecycleIfPrototype(Object object, String key, boolean applyLifecycle) {
    if (applyLifecycle && !isSingleton(key)) {
      try {
        getLifecycleManager().applyCompletedPhases(object);
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not apply lifecycle into prototype object " + key), e);
      }
    }
  }

  @Override
  public <T> Collection<T> lookupObjects(Class<T> type) {
    return lookupByType(type).values();
  }

  @Override
  public <T> Collection<T> lookupLocalObjects(Class<T> type) {
    return internalLookupByTypeWithoutAncestorsAndObjectProviders(type, true, true).values();
  }

  /**
   * For lifecycle we only want spring to return singleton objects from it's application context
   */
  @Override
  public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type) {
    return lookupEntriesForLifecycle(type).values();
  }

  @Override
  public <T> Map<String, T> lookupByType(Class<T> type) {
    return internalLookupByType(type, true, true);
  }

  @Override
  public boolean isSingleton(String key) {
    return applicationContext.isSingleton(key);
  }

  @Override
  public void registerObject(String key, Object value) throws RegistrationException {
    registrationDelegate.registerObject(key, value);
  }

  @Override
  public void registerObject(String key, Object value, Object metadata) throws RegistrationException {
    registrationDelegate.registerObject(key, value, metadata);
  }

  @Override
  public void registerObjects(Map<String, Object> objects) throws RegistrationException {
    registrationDelegate.registerObjects(objects);
  }

  @Override
  protected Object doUnregisterObject(String key) throws RegistrationException {
    return registrationDelegate.unregisterObject(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T inject(T object) {
    try {
      object = getInjectionTarget(requireNonNull(object));
      return initialiseObject((ConfigurableApplicationContext) applicationContext, EMPTY, object);
    } catch (LifecycleException e) {
      throw new MuleRuntimeException(e);
    } catch (Exception e) {
      throw new MuleRuntimeException(new LifecycleException(e, object));
    }
  }

  /**
   * Will fire any lifecycle methods according to the current lifecycle without actually registering the object in the registry.
   * This is useful for prototype objects that are created per request and would clutter the registry with single use objects.
   *
   * @param object the object to process
   * @return the same object with lifecycle methods called (if it has any)
   * @throws MuleException if the registry fails to perform the lifecycle change for the object.
   */
  @Override
  public Object applyLifecycle(Object object) throws MuleException {
    final LifecycleManager lifecycleManager = getLifecycleManager();
    lifecycleManager.applyCompletedPhases(object);
    if (lifecycleManager.getExecutingPhase() != null) {
      lifecycleManager.applyPhase(object, lifecycleManager.getCurrentPhase(), lifecycleManager.getExecutingPhase());
    }
    return object;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object applyLifecycle(Object object, String phase) throws MuleException {
    if (phase == null) {
      getLifecycleManager().applyCompletedPhases(object);
    } else {
      getLifecycleManager().applyPhase(object, NotInLifecyclePhase.PHASE_NAME, phase);
    }
    return object;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void applyLifecycle(Object object, String startPhase, String toPhase) throws MuleException {
    getLifecycleManager().applyPhase(object, startPhase, toPhase);
  }

  @Override
  public synchronized void fireLifecycle(String phase) throws LifecycleException {
    // Avoid trying to register objects while the registry is being shut down.
    synchronized (isStopped) {
      isStopped.set(Stoppable.PHASE_NAME.equals(phase) || Disposable.PHASE_NAME.equals(phase));
    }
    super.fireLifecycle(phase);
  }

  protected final <T> T initialiseObject(ConfigurableApplicationContext applicationContext, String key, T object)
      throws LifecycleException {
    applicationContext.getBeanFactory().autowireBean(object);
    return (T) applicationContext.getBeanFactory().initializeBean(object, key);
  }

  protected <T> Map<String, T> internalLookupByType(Class<T> type, boolean nonSingletons, boolean eagerInit) {
    try {
      Map<String, T> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, type, nonSingletons, eagerInit);
      if (nonSingletons && eagerInit) {
        beans.forEach((key, value) -> applyLifecycleIfPrototype(value, key, true));
      }
      return beans;
    } catch (FatalBeanException fbex) {
      // FBE is a result of a broken config, propagate it (see MULE-3297 for more details)
      String message = format("Failed to lookup beans of type %s from the Spring registry", type);
      throw new MuleRuntimeException(createStaticMessage(message), fbex);
    } catch (Exception e) {
      if (logger.isTraceEnabled()) {
        logger.trace(e.getMessage(), e);
      } else {
        logger.debug(e.getMessage());
      }
      return emptyMap();
    }
  }

  protected abstract <T> Map<String, T> lookupEntriesForLifecycleIncludingAncestors(Class<T> type);

  protected <T> Map<String, T> internalLookupByTypeWithoutAncestorsAndObjectProviders(Class<T> type, boolean nonSingletons,
                                                                                      boolean eagerInit) {
    return internalLookupByTypeWithoutAncestorsAndObjectProviders(type, nonSingletons, eagerInit, applicationContext);
  }

  protected final <T> Map<String, T> internalLookupByTypeWithoutAncestorsAndObjectProviders(Class<T> type, boolean nonSingletons,
                                                                                            boolean eagerInit,
                                                                                            ApplicationContext applicationCtx) {
    try {
      Map<String, T> beans = ((ObjectProviderAwareBeanFactory) applicationCtx.getAutowireCapableBeanFactory())
          .getBeansOfTypeWithObjectProviderObjects(type, nonSingletons, eagerInit);
      if (nonSingletons && eagerInit) {
        beans.forEach((key, value) -> applyLifecycleIfPrototype(value, key, true));
      }
      return beans;
    } catch (FatalBeanException fbex) {
      // FBE is a result of a broken config, propagate it (see MULE-3297 for more details)
      String message = format("Failed to lookup beans of type %s from the Spring registry", type);
      throw new MuleRuntimeException(createStaticMessage(message), fbex);
    } catch (Exception e) {
      if (logger.isTraceEnabled()) {
        logger.trace(e.getMessage(), e);
      } else {
        logger.debug(e.getMessage());
      }
      return emptyMap();
    }
  }

  <T> Map<String, T> lookupEntriesForLifecycle(Class<T> type) {
    return internalLookupByTypeWithoutAncestorsAndObjectProviders(type, false, false, applicationContext);
  }

  @Override
  public Map<String, Object> getDependencies(String key) {
    if (!readOnly) {
      Map<String, Object> dependents = new HashMap<>();
      for (String dependentKey : ((ConfigurableApplicationContext) applicationContext).getBeanFactory()
          .getDependenciesForBean(key)) {
        boolean isBeanDefinition =
            ((ConfigurableApplicationContext) applicationContext).getBeanFactory().containsBeanDefinition(dependentKey);

        if (isBeanDefinition && applicationContext.isSingleton(dependentKey)) {
          dependents.put(dependentKey, get(dependentKey));
        }
      }

      return dependents;
    }

    throw new UnsupportedOperationException("This operation is only available when this registry is backed by a ConfigurableApplicationContext");
  }

  public abstract BeanDependencyResolver getBeanDependencyResolver();

  public abstract ConfigurationDependencyResolver getConfigurationDependencyResolver();

  @Override
  public String[] getBeanNamesForType(Class<?> type) {
    return applicationContext.getBeanNamesForType(type);
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // Registry meta-data
  ////////////////////////////////////////////////////////////////////////////////////

  @Override
  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public boolean isRemote() {
    return false;
  }

  private class ConfigurableRegistrationDelegate implements RegistrationDelegate {

    private final ConfigurableApplicationContext applicationContext;

    private ConfigurableRegistrationDelegate(ConfigurableApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    public void registerObject(String key, Object value) throws RegistrationException {
      try {
        // Avoid trying to register objects while the registry is being stopped.
        synchronized (isStopped) {
          if (isStopped.get()) {
            throw new RegistrationException(createStaticMessage(format(COULD_NOT_ADD_ENTRY_REGISTRY_HAS_BEEN_STOPPED,
                                                                       key)));
          } else {
            doRegisterObject(key, value);
          }
        }
      } catch (RuntimeException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RegistrationException) {
          throw (RegistrationException) cause;
        } else {
          throw new RegistrationException(cause);
        }
      }
    }

    @Override
    public void registerObject(String key, Object value, Object metadata) throws RegistrationException {
      registerObject(key, value);
    }

    @Override
    public void registerObjects(Map<String, Object> objects) throws RegistrationException {
      if (objects == null || objects.isEmpty()) {
        return;
      }

      for (Map.Entry<String, Object> entry : objects.entrySet()) {
        registerObject(entry.getKey(), entry.getValue());
      }
    }

    @Override
    public Object unregisterObject(String key) throws RegistrationException {
      Object object;
      try {
        object = applicationContext.getBean(key);
      } catch (BeanCreationException e) {
        // we might be trying to unregister a bean which had failed with a BeanCreationException
        // In that case, we must be aware that it will fail again here as well.
        object = null;
      }

      if (applicationContext.getBeanFactory().containsBeanDefinition(key)) {
        ((BeanDefinitionRegistry) applicationContext.getBeanFactory()).removeBeanDefinition(key);
      }

      ((DefaultListableBeanFactory) applicationContext.getBeanFactory()).destroySingleton(key);

      return object;
    }

    private void doRegisterObject(String key, Object value) throws RegistrationException {
      if (springContextInitialised.get()) {
        if (applicationContext.containsBean(key)) {
          logger.warn("Spring registry already contains an object named '{}'. The previous object will be overwritten.",
                      key);
          AbstractSpringRegistry.this.unregisterObject(key);
        }

        try {
          value = initialiseObject(applicationContext, key, value);
          applyLifecycle(value);
          applicationContext.getBeanFactory().registerSingleton(key, value);
        } catch (Exception e) {
          throw new RegistrationException(createStaticMessage("Could not register object for key " + key), e);
        }
      } else {
        // since the context has not yet bean initialized, we register a bean definition instead.
        registeredBeanDefinitionsBeforeInitialization.put(key, genericBeanDefinition(ConstantFactoryBean.class)
            .addConstructorArgValue(value).getBeanDefinition());
      }
    }
  }

}
