/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.exception.ExceptionHelper.unwrap;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_PESSIMISTIC_DISPOSE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.api.util.ExceptionUtils;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;
import org.mule.runtime.core.internal.registry.Registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

/**
 * An implementation of {@link LifecycleCallback} for applying {@link Registry} lifecycles
 *
 * @since 3.7.0
 */
public class RegistryLifecycleCallback<T> implements LifecycleCallback<T>, HasLifecycleInterceptor {

  private static final Logger LOGGER = getLogger(RegistryLifecycleCallback.class);

  protected final RegistryLifecycleManager registryLifecycleManager;
  private LifecycleInterceptor interceptor = new NullLifecycleInterceptor();

  public RegistryLifecycleCallback(RegistryLifecycleManager registryLifecycleManager) {
    this.registryLifecycleManager = registryLifecycleManager;
  }

  @Override
  public void onTransition(String phaseName, T object) throws MuleException {
    try {
      registryLifecycleManager.muleContext.withLifecycleLock((CheckedRunnable) () -> doOnTransition(phaseName, object));
    } catch (RuntimeException e) {
      MuleException muleException = ExceptionUtils.extractOfType(e, MuleException.class).orElse(null);
      if (muleException != null) {
        throw muleException;
      }

      throw new MuleRuntimeException(unwrap(e));
    }
  }

  private void doOnTransition(String phaseName, T object) throws MuleException {

    LifecyclePhase phase = registryLifecycleManager.phases.get(phaseName);

    LOGGER.debug("Applying lifecycle phase: {} for registry: {}", phase, object.getClass().getSimpleName());

    doApplyLifecycle(phase, new HashSet<>(), registryLifecycleManager.getObjectsForPhase(phase));
    interceptor.onPhaseCompleted(phase);
  }

  private void doApplyLifecycle(LifecyclePhase phase, Set<Object> duplicates, Collection<?> targetObjects)
      throws LifecycleException {
    if (CollectionUtils.isEmpty(targetObjects)) {
      return;
    }

    for (Object target : targetObjects) {
      if (target == null || duplicates.contains(target)) {
        continue;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("lifecycle phase: " + phase.getName() + " for object: " + target);
      }

      applyLifecycle(phase, duplicates, target);
    }
  }

  private void applyLifecycle(LifecyclePhase phase, Set<Object> duplicates, Object target) throws LifecycleException {
    try {
      if (interceptor.beforePhaseExecution(phase, target)) {
        phase.applyLifecycle(target);
        duplicates.add(target);
        interceptor.afterPhaseExecution(phase, target, empty());
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format(
                              "Skipping the application of the '%s' lifecycle phase over a certain object "
                                  + "because a %s interceptor of type [%s] indicated so. Object is: %s",
                              phase.getName(), LifecycleInterceptor.class.getSimpleName(),
                              interceptor.getClass().getName(), target));
        }
      }
    } catch (Exception e) {
      interceptor.afterPhaseExecution(phase, target, of(e));
      if (getProperty(MULE_LIFECYCLE_PESSIMISTIC_DISPOSE) == null
          && (phase.getName().equals(Disposable.PHASE_NAME) || phase.getName().equals(Stoppable.PHASE_NAME))) {
        LOGGER.info(format("Failure executing phase %s over object %s, error message is: %s", phase.getName(), target,
                           e.getMessage()),
                    e.getMessage());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(e.getMessage(), e);
        }
      } else {
        throw e;
      }
    }
  }

  @Override
  public void setLifecycleInterceptor(LifecycleInterceptor interceptor) {
    this.interceptor = interceptor;
  }

}
