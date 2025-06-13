/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import static org.mule.runtime.api.exception.ExceptionHelper.unwrap;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;

import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;
import org.mule.runtime.core.internal.registry.Registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
    final Runnable transitionCommand = () -> {
      try {
        doOnTransition(phaseName, object);
      } catch (LifecycleException e) {
        throw new MuleRuntimeException(e);
      }
    };

    try {
      registryLifecycleManager.getMuleContext()
          .map(muleContext -> (Runnable) () -> muleContext.withLifecycleLock(transitionCommand))
          .orElse(transitionCommand)
          .run();
    } catch (RuntimeException e) {
      MuleException muleException = extractOfType(e, MuleException.class).orElse(null);
      if (muleException != null) {
        throw muleException;
      }

      throw new MuleRuntimeException(unwrap(e));
    }
  }

  private void doOnTransition(String phaseName, T object) throws LifecycleException {

    LifecyclePhase phase = registryLifecycleManager.getPhase(phaseName);

    LOGGER.debug("Applying lifecycle phase: {} for registry: {}", phase, object.getClass().getSimpleName());

    doApplyLifecycle(phase, new HashSet<>(), registryLifecycleManager.getObjectsForPhase(phase));

    interceptor.onPhaseCompleted(phase);
  }

  private void doApplyLifecycle(LifecyclePhase phase, Set<Object> duplicates, Collection<?> targetObjects)
      throws LifecycleException {
    for (Object target : targetObjects) {
      if (target == null || duplicates.contains(target)) {
        continue;
      }
      LOGGER.debug("lifecycle phase: {} for object: {}", phase.getName(), target.getClass().getSimpleName());
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
        LOGGER.debug("Skipping the application of the '{}' lifecycle phase over a certain object "
            + "because a {} interceptor of type [{}] indicated so. Object is: {}",
                     phase.getName(), LifecycleInterceptor.class.getSimpleName(),
                     interceptor.getClass().getName(), target.getClass().getSimpleName());
      }
    } catch (Exception e) {
      interceptor.afterPhaseExecution(phase, target, of(e));
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) == null
          && (phase.getName().equals(Disposable.PHASE_NAME) || phase.getName().equals(Stoppable.PHASE_NAME))) {
        LOGGER.info("Failure executing phase {} over object {}{}, error is: {}({})",
                    phase.getName(),
                    target.getClass().getSimpleName(),
                    target instanceof Component ? (": " + ((Component) target).getRepresentation()) : "",
                    e.getClass().getName(),
                    e.getMessage());
        LOGGER.atDebug().setCause(e).log(e.getMessage());
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
