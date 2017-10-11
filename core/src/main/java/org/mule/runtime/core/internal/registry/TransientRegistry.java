/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.registry.map.RegistryMap;
import org.mule.runtime.core.privileged.endpoint.LegacyImmutableEndpoint;
import org.mule.runtime.core.privileged.registry.InjectProcessor;
import org.mule.runtime.core.privileged.registry.ObjectProcessor;
import org.mule.runtime.core.privileged.registry.PreInitProcessor;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transport.LegacyConnector;

import org.apache.commons.collections.functors.InstanceofPredicate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Use the registryLock when reading/writing/iterating over the contents of the registry hashmap.
 *
 * @deprecated as of 3.7.0. Use {@link SimpleRegistry instead}.
 */
@Deprecated
public class TransientRegistry extends AbstractRegistry {

  public static final String REGISTRY_ID = "org.mule.runtime.core.Registry.Transient";

  private final RegistryMap registryMap = new RegistryMap(logger);

  public TransientRegistry(String id, MuleContext muleContext, LifecycleInterceptor lifecycleInterceptor) {
    super(id, muleContext, lifecycleInterceptor);
    putDefaultEntriesIntoRegistry();
  }

  private void putDefaultEntriesIntoRegistry() {
    Map<String, Object> processors = new HashMap<>();
    processors.put("_muleContextProcessor", new MuleContextProcessor(muleContext));
    processors.put("_registryProcessor", new RegistryProcessor(muleContext));
    processors.put("_muleLifecycleStateInjectorProcessor", new LifecycleStateInjectorProcessor(getLifecycleManager().getState()));
    processors.put("_muleLifecycleManager", getLifecycleManager());
    registryMap.putAll(processors);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    applyProcessors(lookupObjects(LegacyConnector.class), null);
    applyProcessors(lookupObjects(Transformer.class), null);
    applyProcessors(lookupObjects(LegacyImmutableEndpoint.class), null);
    applyProcessors(lookupObjects(Object.class), null);
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

  protected Map<String, Object> applyProcessors(Map<String, Object> objects) {
    if (objects == null || !isInitialised()) {
      return null;
    }

    Map<String, Object> results = new HashMap<>();
    for (Map.Entry<String, Object> entry : objects.entrySet()) {
      // We do this inside the loop in case the map contains ObjectProcessors
      Collection<ObjectProcessor> processors = lookupObjects(ObjectProcessor.class);
      for (ObjectProcessor processor : processors) {
        Object result = processor.process(entry.getValue());
        if (result != null) {
          results.put(entry.getKey(), result);
        }
      }
    }
    return results;
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
  public <T> T lookupObject(String key) {
    return doGet(key);
  }

  @Override
  public <T> T lookupObject(Class<T> type) throws RegistrationException {
    return super.lookupObject(type);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Collection<T> lookupObjects(Class<T> returntype) {
    return (Collection<T>) registryMap.select(new InstanceofPredicate(returntype));
  }

  @Override
  public <T> Collection<T> lookupLocalObjects(Class<T> type) {
    // just delegate to lookupObjects since there's no parent ever
    return lookupObjects(type);
  }

  @Override
  public boolean isSingleton(String key) {
    return true;
  }

  /**
   * Will fire any lifecycle methods according to the current lifecycle without actually registering the object in the registry.
   * This is useful for prototype objects that are created per request and would clutter the registry with single use objects.
   *
   * @param object the object to process
   * @return the same object with lifecycle methods called (if it has any)
   * @throws MuleException if the registry fails to perform the lifecycle change for the object.
   */
  Object applyLifecycle(Object object) throws MuleException {
    getLifecycleManager().applyCompletedPhases(object);
    return object;
  }

  Object applyLifecycle(Object object, String phase) throws MuleException {
    getLifecycleManager().applyPhase(object, NotInLifecyclePhase.PHASE_NAME, phase);
    return object;
  }

  protected Object applyProcessors(Object object, Object metadata) {
    Object theObject = object;

    if (!hasFlag(metadata, MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG)) {
      // Process injectors first
      Collection<InjectProcessor> injectProcessors = lookupObjects(InjectProcessor.class);
      for (InjectProcessor processor : injectProcessors) {
        theObject = processor.process(theObject);
      }
    }

    if (!hasFlag(metadata, MuleRegistry.PRE_INIT_PROCESSORS_BYPASS_FLAG)) {
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

  /**
   * Allows for arbitary registration of transient objects
   *
   * @param key
   * @param value
   */
  @Override
  public void registerObject(String key, Object value) throws RegistrationException {
    registerObject(key, value, Object.class);
  }

  /**
   * Allows for arbitrary registration of transient objects
   */
  @Override
  public void registerObject(String key, Object object, Object metadata) throws RegistrationException {
    checkDisposed();
    if (StringUtils.isBlank(key)) {
      throw new RegistrationException(I18nMessageFactory.createStaticMessage("Attempt to register object with no key"));
    }

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("registering key/object %s/%s", key, object));
    }

    logger.debug("applying processors");
    object = applyProcessors(object, metadata);
    if (object == null) {
      return;
    }

    doRegisterObject(key, object, metadata);
  }

  protected <T> T doGet(String key) {
    return registryMap.get(key);
  }

  protected void doRegisterObject(String key, Object object, Object metadata) throws RegistrationException {
    doPut(key, object);

    try {
      if (!hasFlag(metadata, MuleRegistry.LIFECYCLE_BYPASS_FLAG)) {
        if (logger.isDebugEnabled()) {
          logger.debug("applying lifecycle to object: " + object);
        }
        getLifecycleManager().applyCompletedPhases(object);
      }
    } catch (MuleException e) {
      throw new RegistrationException(e);
    }
  }

  protected void doPut(String key, Object object) {
    registryMap.putAndLogWarningIfDuplicate(key, object);
  }

  protected void checkDisposed() throws RegistrationException {
    if (getLifecycleManager().isPhaseComplete(Disposable.PHASE_NAME)) {
      throw new RegistrationException(I18nMessageFactory
          .createStaticMessage("Cannot register objects on the registry as the context is disposed"));
    }
  }

  protected boolean hasFlag(Object metaData, int flag) {
    return !(metaData == null || !(metaData instanceof Integer)) && ((Integer) metaData & flag) != 0;
  }

  @Override
  protected Object doUnregisterObject(String key) throws RegistrationException {
    return registryMap.remove(key);
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
