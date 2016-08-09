/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.lifecycle;

import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.LifecycleInterceptor;
import org.mule.runtime.core.api.lifecycle.LifecyclePhase;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LifecycleInterceptor} which tracks the completion of the {@link Initialisable} phase so that if it fails to complete,
 * the {@link Disposable} phase is only applied on the target objects on which it could be successfully applied.
 *
 * @since 3.8
 */
public class InitDisposeLifecycleInterceptor implements LifecycleInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(InitDisposeLifecycleInterceptor.class);

  private Set<Integer> initialisedObjects = new HashSet<>();
  private boolean initialiseCompleted = false;

  /**
   * Evaluates the following conditions in order to determine if the phase should be skipped:
   * <p>
   * <ul>
   * <li>{@code phase} name is {@link Disposable#PHASE_NAME}</li>
   * <li>The {@link Initialisable} phase was not successfully completed</li>
   * <li>The given {@code object} is {@link Initialisable} yet it wasn't initialised</li>
   * </ul>
   * <p>
   * If all three of the above conditions are met, then this method returns {@code false}. Otherwise, it returns {@code true}
   *
   * @param phase the phase being applied
   * @param object the target object
   * @return whether the {@code phase} should be applied on the {@code object}
   */
  @Override
  public boolean beforeLifecycle(LifecyclePhase phase, Object object) {
    if (initialiseCompleted) {
      return true;
    }

    if (isDispose(phase) && (object instanceof Initialisable)) {
      if (!initialisedObjects.contains(getLifecycleTrackingKey(object))) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(String.format("Skipping %s lifecycle phase on object because %s phase failed before it could be applied "
              + "on it. Object is: %s", Disposable.PHASE_NAME, Initialisable.PHASE_NAME, object));
        }
        return false;
      }
    }

    return true;
  }

  /**
   * If the {@code phase} name is {@link Initialisable#PHASE_NAME} then it tracks the given {@code object} as initialised.
   *
   * @param phase the phase that was applied
   * @param object the target object
   */
  @Override
  public void afterLifecycle(LifecyclePhase phase, Object object) {
    if (isInitialise(phase) && !initialiseCompleted) {
      initialisedObjects.add(getLifecycleTrackingKey(object));
    }
  }

  /**
   * If the {@code phase} name is {@link Initialisable#PHASE_NAME} then it marks the {@link Initialisable} phase as completed and
   * clears all other tracking state so that memory can be reclaimed.
   *
   * @param phase the phase that was applied
   */
  @Override
  public void onPhaseCompleted(LifecyclePhase phase) {
    if (isInitialise(phase)) {
      initialiseCompleted = true;
      initialisedObjects = null;
    }
  }

  private int getLifecycleTrackingKey(Object object) {
    return System.identityHashCode(object);
  }

  private boolean isInitialise(LifecyclePhase phase) {
    return Initialisable.PHASE_NAME.equals(phase.getName());
  }

  private boolean isDispose(LifecyclePhase phase) {
    return Disposable.PHASE_NAME.equals(phase.getName());
  }
}
