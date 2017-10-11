/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

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
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.config.CustomService;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;
import org.mule.runtime.core.internal.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.internal.lifecycle.RegistryLifecycleManager;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractRegistry implements Registry {

  /**
   * the unique id for this Registry
   */
  private String id;

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

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
    getLifecycleManager().fireLifecycle(phase);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) lookupObject(key); // do not remove this cast, the CI server fails to compile the code without it
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

  /**
   * Template method for the logic to actually unregister the key without applying any lifecycle to it. Applying the shutdown
   * lifecycle will be up to {@link #unregisterObject(String)}
   *
   * @param key the key of the object to be unregistered object
   * @return the object which was registered under {@code key}
   * @throws RegistrationException
   */
  protected abstract Object doUnregisterObject(String key) throws RegistrationException;

  @Override
  public <T> T lookupObject(Class<T> type) throws RegistrationException {
    // Accumulate objects from all registries.
    Collection<T> objects = lookupObjects(type);

    if (objects.size() == 1) {
      return objects.iterator().next();
    } else if (objects.size() > 1) {
      throw new RegistrationException(createStaticMessage("More than one object of type %s registered but only one expected. Objects found are: %s",
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


  // /////////////////////////////////////////////////////////////////////////
  // Registry Metadata
  // /////////////////////////////////////////////////////////////////////////

  @Override
  public final String getRegistryId() {
    return id;
  }
}
