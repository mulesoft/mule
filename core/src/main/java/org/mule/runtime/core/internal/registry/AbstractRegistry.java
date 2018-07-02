/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleManager;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.internal.registry.map.RegistryMap;
import org.mule.runtime.core.privileged.endpoint.LegacyImmutableEndpoint;
import org.mule.runtime.core.privileged.registry.InjectProcessor;
import org.mule.runtime.core.privileged.registry.PreInitProcessor;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transport.LegacyConnector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.functors.InstanceofPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractRegistry implements Registry {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * the unique id for this Registry
   */
  private String id;

  private final RegistryMap registryMap = new RegistryMap(logger);

  protected MuleContext muleContext;
  private RegistryLifecycleManager lifecycleManager;

  protected AbstractRegistry(String id, MuleContext muleContext, LifecycleInterceptor lifecycleInterceptor) {
    if (id == null) {
      throw new MuleRuntimeException(CoreMessages.objectIsNull("RegistryID"));
    }
    this.id = id;
    this.muleContext = muleContext;
    lifecycleManager =
        (RegistryLifecycleManager) createLifecycleManager(lifecycleInterceptor);
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
  public final synchronized void dispose() {
    if (lifecycleManager.getState().isStarted()) {
      try {
        getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);
      } catch (LifecycleException e) {
        logger.error("Failed to shut down registry cleanly: " + getRegistryId(), e);
      }
    }
    // Fire dispose lifecycle before calling doDispose() that that registries can clear any object caches once all objects
    // are disposed
    try {
      getLifecycleManager().fireLifecycle(Disposable.PHASE_NAME);
    } catch (LifecycleException e) {
      logger.error("Failed to shut down registry cleanly: " + getRegistryId(), e);
    }

    disposeLostObjects();
    registryMap.clear();

    try {
      doDispose();
    } catch (Exception e) {
      logger.error("Failed to cleanly dispose: " + e.getMessage(), e);
    }
  }

  protected LifecycleManager createLifecycleManager(LifecycleInterceptor lifecycleInterceptor) {
    // TODO(pablo.kraan): MULE-12609 - using LifecycleManager to avoid exposing RegistryLifecycleManager
    return new RegistryLifecycleManager(getRegistryId(), this, muleContext, lifecycleInterceptor);
  }

  abstract protected void doInitialise() throws InitialisationException;

  abstract protected void doDispose();

  @Override
  public final void initialise() throws InitialisationException {
    if (id == null) {
      logger.warn("No unique id has been set on this registry");
      id = UUID.getUUID();
    }

    applyProcessors(lookupObjects(LegacyConnector.class), null);
    applyProcessors(lookupObjects(Transformer.class), null);
    applyProcessors(lookupObjects(LegacyImmutableEndpoint.class), null);
    applyProcessors(lookupObjects(Object.class), null);

    try {
      doInitialise();
    } catch (InitialisationException e) {
      throw e;
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
    try {
      fireLifecycle(Initialisable.PHASE_NAME);
    } catch (InitialisationException e) {
      throw e;
    } catch (LifecycleException e) {
      if (e.getComponent() instanceof Initialisable) {
        throw new InitialisationException(e, (Initialisable) e.getComponent());
      }
      throw new InitialisationException(e, this);
    }
  }

  protected boolean isInitialised() {
    return getLifecycleManager().getState().isInitialised();
  }

  public LifecycleManager getLifecycleManager() {
    return lifecycleManager;
  }

  @Override
  public void fireLifecycle(String phase) throws LifecycleException {
    // Implicitly call stop if necessary when disposing
    if (Disposable.PHASE_NAME.equals(phase) && lifecycleManager.getState().isStarted()) {
      getLifecycleManager().fireLifecycle(Stoppable.PHASE_NAME);
    }
    // Don't fire lifecycle phase if it's Stop and the current state was not started
    if (!Stoppable.PHASE_NAME.equals(phase) || lifecycleManager.getState().isStarted()) {
      getLifecycleManager().fireLifecycle(phase);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) lookupObject(key); // do not remove this cast, the CI server fails to compile the code without it
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
      throw new RegistrationException(createStaticMessage("Attempt to register object with no key"));
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
    getLifecycleManager().applyCompletedPhases(object);
    return object;
  }

  @Override
  public Object applyLifecycle(Object object, String phase) throws MuleException {
    getLifecycleManager().applyPhase(object, NotInLifecyclePhase.PHASE_NAME, phase);
    return object;
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
  public final Object unregisterObject(String key) throws RegistrationException {
    Object object = doUnregisterObject(key);

    try {
      getLifecycleManager().applyPhase(object, getLifecycleManager().getCurrentPhase(), Disposable.PHASE_NAME);
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn(String.format("Could not apply shutdown lifecycle to object '%s' after being unregistered.", key), e);
      }
    }

    return object;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public final Object unregisterObject(String key, Object metadata) throws RegistrationException {
    return unregisterObject(key);
  }

  @Override
  public <T> T lookupObject(Class<T> type) throws RegistrationException {
    // Accumulate objects from all registries.
    Collection<T> objects = lookupObjects(type);

    if (objects.size() == 1) {
      return objects.iterator().next();
    } else if (objects.size() > 1) {
      throw new RegistrationException(
          createStaticMessage("More than one object of type %s registered but only one expected. Objects found are: %s",
                              type, objects.toString()));
    } else {
      return null;
    }
  }

  @Override
  public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type) {
    // By default use the normal lookup. If a registry implementation needs a
    // different lookup implementation for lifecycle it should override this
    // method
    return lookupObjects(type);
  }

  @Override
  public <T> T lookupObject(String key) {
    return registryMap.get(key);
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



  // /////////////////////////////////////////////////////////////////////////
  // Registry Metadata
  // /////////////////////////////////////////////////////////////////////////

  @Override
  public final String getRegistryId() {
    return id;
  }

  @Override
  public boolean isSingleton(String key) {
    return true;
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

  /**
   * Template method for the logic to actually unregister the key without applying any lifecycle to it. Applying the shutdown
   * lifecycle will be up to {@link #unregisterObject(String)}
   *
   * @param key the key of the object to be unregistered object
   * @return the object which was registered under {@code key}
   * @throws RegistrationException
   */
  protected Object doUnregisterObject(String key) throws RegistrationException {
    return registryMap.remove(key);
  }

  protected boolean hasFlag(Object metaData, int flag) {
    return !(metaData == null || !(metaData instanceof Integer)) && ((Integer) metaData & flag) != 0;
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

  protected void checkDisposed() throws RegistrationException {
    if (getLifecycleManager().isPhaseComplete(Disposable.PHASE_NAME)) {
      throw new RegistrationException(createStaticMessage("Cannot register objects on the registry as the context is disposed"));
    }
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isRemote() {
    return false;
  }
}
