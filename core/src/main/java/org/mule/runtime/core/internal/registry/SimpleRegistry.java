/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_APPLY_OBJECT_PROCESSOR;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.getInstance;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_HANDLER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.internal.registry.MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG;
import static org.mule.runtime.core.internal.registry.MuleRegistry.PRE_INIT_PROCESSORS_BYPASS_FLAG;
import static org.mule.runtime.core.internal.util.InjectionUtils.getInjectionTarget;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.FeatureContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.config.FeatureFlaggingServiceBuilder;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.registry.map.RegistryMap;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.endpoint.LegacyImmutableEndpoint;
import org.mule.runtime.core.privileged.registry.InjectProcessor;
import org.mule.runtime.core.privileged.registry.PreInitProcessor;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * A very simple implementation of {@link Registry}. Useful for starting really lightweight contexts which don't depend on heavier
 * object containers such as Spring or Guice (testing being the best example).
 * <p/>
 * The {@link #inject(Object)} operation will only consider fields annotated with {@link Inject} and will perform the injection
 * using simple, not-cached reflection. Also, initialisation lifecycle will be performed in pseudo-random order, no analysis will
 * be done to ensure that dependencies of a given object get their lifecycle before it.
 *
 * @since 3.7.0
 */
public class SimpleRegistry extends AbstractRegistry implements Injector {

  private static final String REGISTRY_ID = "org.mule.runtime.core.Registry.Simple";

  private final boolean disabledApplyObjectProcessor;
  private final RegistryMap registryMap = new RegistryMap(logger);

  public SimpleRegistry(MuleContext muleContext, LifecycleInterceptor lifecycleInterceptor) {
    super(REGISTRY_ID, muleContext, lifecycleInterceptor);
    putDefaultEntriesIntoRegistry();

    disabledApplyObjectProcessor =
        ((FeatureFlaggingService) registryMap.get(FEATURE_FLAGGING_SERVICE_KEY)).isEnabled(DISABLE_APPLY_OBJECT_PROCESSOR);
  }

  private void putDefaultEntriesIntoRegistry() {
    Map<String, Object> defaultEntries = new HashMap<>();
    if (muleContext != null) {
      defaultEntries.put(OBJECT_MULE_CONTEXT, muleContext);
      defaultEntries.put(OBJECT_REGISTRY, new DefaultRegistry(muleContext));
      defaultEntries.put(OBJECT_NOTIFICATION_HANDLER, ((PrivilegedMuleContext) muleContext).getNotificationManager());
      defaultEntries.put("_muleContextProcessor", new MuleContextProcessor(muleContext));
      // Initial feature flagging service setup
      FeatureFlaggingRegistry ffRegistry = getInstance();
      FeatureFlaggingService featureFlaggingService = new FeatureFlaggingServiceBuilder()
          .withContext(muleContext)
          .withContext(new FeatureContext(muleContext.getConfiguration().getMinMuleVersion().orElse(null), resolveArtifactName()))
          .withMuleContextFlags(ffRegistry.getFeatureConfigurations())
          .withFeatureContextFlags(ffRegistry.getFeatureFlagConfigurations())
          .build();
      defaultEntries.put(FEATURE_FLAGGING_SERVICE_KEY, featureFlaggingService);
    }

    defaultEntries.put("_muleLifecycleStateInjectorProcessor",
                       new LifecycleStateInjectorProcessor(getLifecycleManager().getState()));
    defaultEntries.put("_muleLifecycleManager", getLifecycleManager());

    registryMap.putAll(defaultEntries);
  }

  private String resolveArtifactName() {
    if (muleContext.getConfiguration() != null) {
      return muleContext.getConfiguration().getId();
    } else {
      return "";
    }
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    injectFieldDependencies();
    injectInto(lookupObjects(Transformer.class));
    injectInto(lookupObjects(LegacyImmutableEndpoint.class));
    injectInto(lookupObjects(Object.class));
  }

  @Override
  protected void doDispose() {
    disposeLostObjects();
    registryMap.clear();
  }

  private void disposeLostObjects() {
    for (Object obj : registryMap.getLostObjects()) {
      try {
        ((Disposable) obj).dispose();
      } catch (Exception e) {
        logger.warn("Can not dispose object. " + getMessage(e));
        if (logger.isDebugEnabled()) {
          logger.debug("Can not dispose object. " + getStackTrace(e));
        }
      }
    }
  }

  /**
   * This implementation doesn't support applying lifecycle upon lookup and thus this method simply delegates into
   * {@link #lookupObject(String)}
   */
  @Override
  public <T> T lookupObject(String key, boolean applyLifecycle) {
    return lookupObject(key);
  }

  @Override
  public <T> T lookupObject(String key) {
    return doGet(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Collection<T> lookupObjects(Class<T> returntype) {
    return (Collection<T>) registryMap.select(returntype::isInstance);
  }

  @Override
  public <T> Collection<T> lookupLocalObjects(Class<T> type) {
    // just delegate to lookupObjects since there's no parent ever
    return lookupObjects(type);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> lookupByType(Class<T> type) {
    final Map<String, T> results = new HashMap<>();
    try {
      registryMap.lockForReading();

      for (Map.Entry<String, Object> entry : registryMap.entrySet()) {
        final Class<?> clazz = entry.getValue().getClass();
        if (type.isAssignableFrom(clazz)) {
          results.put(entry.getKey(), (T) entry.getValue());
        }
      }
    } finally {
      registryMap.unlockForReading();
    }

    return results;
  }

  @Override
  public boolean isSingleton(String key) {
    return true;
  }

  @Override
  public void registerObjects(Map<String, Object> objects) throws RegistrationException {
    if (objects == null) {
      return;
    }

    for (Map.Entry<String, Object> entry : objects.entrySet()) {
      registerObject(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Allows for arbitrary registration of objects
   *
   * @param key
   * @param value
   */
  @Override
  public void registerObject(String key, Object object) throws RegistrationException {
    doRegisterObject(key, preRegisterObject(key, object, this::injectInto));
  }

  /**
   * Allows for arbitrary registration of objects
   */
  @Override
  public void registerObject(String key, Object object, Object metadata) throws RegistrationException {
    doRegisterObject(key, preRegisterObject(key, object, o -> applyProcessors(o, metadata)));
  }

  private Object preRegisterObject(String key, Object object, UnaryOperator<Object> processor) throws RegistrationException {
    checkDisposed();
    if (StringUtils.isBlank(key)) {
      throw new RegistrationException(createStaticMessage("Attempt to register object with no key"));
    }

    if (logger.isDebugEnabled()) {
      logger.debug(format("registering key/object %s/%s", key, object));
    }

    logger.debug("applying processors");

    return processor.apply(object);
  }

  private void checkDisposed() throws RegistrationException {
    if (getLifecycleManager().isPhaseComplete(Disposable.PHASE_NAME)) {
      throw new RegistrationException(createStaticMessage("Cannot register objects on the registry as the context is disposed"));
    }
  }

  private void doRegisterObject(String key, Object object) throws RegistrationException {
    Object previous = doGet(key);
    if (previous != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(format("An entry already exists for key %s. It will be replaced", key));
      }

      unregisterObject(key);
    }

    doPut(key, object);

    try {
      getLifecycleManager().applyCompletedPhases(object);
    } catch (MuleException e) {
      throw new RegistrationException(e);
    }
  }

  private <T> T doGet(String key) {
    return registryMap.get(key);
  }

  private void doPut(String key, Object object) {
    registryMap.putAndLogWarningIfDuplicate(key, object);
  }

  @Override
  protected Object doUnregisterObject(String key) throws RegistrationException {
    return registryMap.remove(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object applyLifecycle(Object object) throws MuleException {
    getLifecycleManager().applyCompletedPhases(object);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T inject(T object) {
    object = getInjectionTarget(object);
    return (T) injectInto(applyFlaggedProcessors(object, Object.class));
  }

  private Object applyProcessors(Object object, Object metadata) {
    return injectInto(applyFlaggedProcessors(object, metadata));
  }

  // TODO W- Remove this
  private Object applyFlaggedProcessors(Object object, Object metadata) {
    if (disabledApplyObjectProcessor) {
      return object;
    }

    Object theObject = object;

    if (!hasFlag(metadata, INJECT_PROCESSORS_BYPASS_FLAG)) {
      // Process injectors first
      Collection<InjectProcessor> injectProcessors = lookupObjects(InjectProcessor.class);
      for (InjectProcessor processor : injectProcessors) {
        theObject = processor.process(theObject);
      }
    }

    if (!hasFlag(metadata, PRE_INIT_PROCESSORS_BYPASS_FLAG)) {
      // Then any other processors
      Collection<PreInitProcessor> processors = lookupObjects(PreInitProcessor.class);
      for (PreInitProcessor processor : processors) {
        theObject = processor.process(theObject);
        if (theObject == null) {
          return null;
        }
      }
    }
    return theObject;
  }

  protected boolean hasFlag(Object metaData, int flag) {
    return metaData != null && metaData instanceof Integer && ((Integer) metaData & flag) != 0;
  }

  private void injectFieldDependencies() throws InitialisationException {
    lookupObjects(Object.class).forEach(this::injectInto);
  }

  private <T> T injectInto(T object) {
    for (Field field : getAllFields(object.getClass(), withAnnotation(Inject.class))) {
      try {
        Object dependency = resolveTypedDependency(field.getType(), field.getAnnotation(Named.class),
                                                   () -> ((ParameterizedType) (field.getGenericType()))
                                                       .getActualTypeArguments()[0]);

        field.setAccessible(true);
        if (dependency != null) {
          field.set(object, dependency);
        }
      } catch (Exception e) {
        throw new RuntimeException(format("Could not inject dependency on field %s of type %s", field.getName(),
                                          object.getClass().getName()),
                                   e);
      }
    }
    for (Method method : getAllMethods(object.getClass(), withAnnotation(Inject.class))) {
      if (method.getParameters().length == 1) {
        try {
          Object dependency = resolveTypedDependency(method.getParameterTypes()[0], method.getAnnotation(Named.class),
                                                     () -> ((ParameterizedType) (method.getGenericParameterTypes()[0]))
                                                         .getActualTypeArguments()[0]);

          if (dependency != null) {
            method.invoke(object, dependency);
          }
        } catch (Exception e) {
          throw new RuntimeException(format("Could not inject dependency on method %s of type %s", method.getName(),
                                            object.getClass().getName()),
                                     e);
        }
      }

    }
    return object;
  }

  private Object resolveTypedDependency(Class<?> dependencyType, final Named namedAnnotation, Supplier<Type> typeSupplier)
      throws RegistrationException {
    boolean nullToOptional = false;
    boolean collection = false;
    if (dependencyType.equals(Optional.class)) {
      nullToOptional = true;
    } else if (Collection.class.isAssignableFrom(dependencyType)) {
      collection = true;
    }

    if (nullToOptional || collection) {
      Type type = typeSupplier.get();
      if (type instanceof ParameterizedType) {
        dependencyType = (Class<?>) ((ParameterizedType) type).getRawType();
      } else {
        dependencyType = (Class<?>) type;
      }
    }

    return resolveDependency(dependencyType, nullToOptional, collection, namedAnnotation);
  }

  private Object resolveDependency(Class<?> dependencyType, boolean nullToOptional, boolean collection, Named nameAnnotation)
      throws RegistrationException {
    if (collection) {
      return resolveObjectsToInject(dependencyType);
    } else {
      return resolveObjectToInject(dependencyType, nameAnnotation != null ? nameAnnotation.value() : null, nullToOptional);
    }
  }

  private Object resolveObjectToInject(Class<?> dependencyType, String name, boolean nullToOptional)
      throws RegistrationException {
    Object dependency;
    if (name != null) {
      dependency = lookupObject(name);
    } else {
      dependency = lookupObject(dependencyType);
    }
    if (dependency == null && MuleContext.class.isAssignableFrom(dependencyType)) {
      dependency = muleContext;
    }
    return nullToOptional ? ofNullable(dependency) : dependency;
  }

  private <T> Collection<T> resolveObjectsToInject(Class<T> dependencyType)
      throws RegistrationException {
    Collection<T> dependencies = lookupObjects(dependencyType);
    return dependencies;
  }

  // /////////////////////////////////////////////////////////////////////////
  // Registry Metadata
  // /////////////////////////////////////////////////////////////////////////

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isRemote() {
    return false;
  }
}
