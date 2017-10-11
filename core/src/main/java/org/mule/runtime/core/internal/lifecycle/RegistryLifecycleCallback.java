/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.api.lifecycle.LifecycleObject;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.lifecycle.phases.ContainerManagedLifecyclePhase;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;
import org.mule.runtime.core.internal.registry.Registry;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
    LifecyclePhase phase = registryLifecycleManager.phases.get(phaseName);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Applying lifecycle phase: %s for registry: %s", phase, object.getClass().getSimpleName()));
    }

    if (phase instanceof ContainerManagedLifecyclePhase) {
      phase.applyLifecycle(object);
      return;
    }

    // overlapping interfaces can cause duplicates
    // TODO: each LifecycleManager should keep this set per executing phase
    // and clear it when the phase is fully applied
    Set<Object> duplicates = new HashSet<>();

    final NotificationDispatcher notificationFirer = ((MuleContextWithRegistries) registryLifecycleManager.muleContext)
        .getRegistry().lookupObject(NotificationDispatcher.class);
    for (LifecycleObject lifecycleObject : phase.getOrderedLifecycleObjects()) {
      lifecycleObject.firePreNotification(notificationFirer);

      // TODO Collection -> List API refactoring
      Collection<?> targetsObj = lookupObjectsForLifecycle(lifecycleObject);
      doApplyLifecycle(phase, duplicates, lifecycleObject, targetsObj);
      lifecycleObject.firePostNotification(notificationFirer);
    }

    interceptor.onPhaseCompleted(phase);
  }

  private void doApplyLifecycle(LifecyclePhase phase, Set<Object> duplicates, LifecycleObject lifecycleObject,
                                Collection<?> targetObjects)
      throws LifecycleException {
    if (CollectionUtils.isEmpty(targetObjects)) {
      return;
    }

    for (Object target : targetObjects) {
      if (duplicates.contains(target) || target == null) {
        continue;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("lifecycle phase: " + phase.getName() + " for object: " + target);
      }

      applyLifecycle(phase, duplicates, target);
    }

    // the target object might have created and registered a new object
    // (e.g.: an endpoint which registers a connector)
    // check if there're new objects for the phase
    int originalTargetCount = targetObjects.size();
    targetObjects = lookupObjectsForLifecycle(lifecycleObject);
    if (targetObjects.size() > originalTargetCount) {
      doApplyLifecycle(phase, duplicates, lifecycleObject, targetObjects);
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
      if (phase.getName().equals(Disposable.PHASE_NAME) || phase.getName().equals(Stoppable.PHASE_NAME)) {
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

  protected Collection<?> lookupObjectsForLifecycle(LifecycleObject lo) {
    return registryLifecycleManager.getLifecycleObject().lookupObjectsForLifecycle(lo.getType());
  }

  @Override
  public void setLifecycleInterceptor(LifecycleInterceptor interceptor) {
    this.interceptor = interceptor;
  }
}
