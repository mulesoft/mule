/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LifecycleInterceptor} which tracks the completion of the a phase so that if it fails to complete, the another phase is
 * only applied on the target objects on which it could be successfully applied. It also checks that the final phase is only
 * applied if the initial phase was applied.
 *
 * @since 3.8
 */
public class DefaultLifecycleInterceptor implements LifecycleInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLifecycleInterceptor.class);

  private Map<Object, Object> trackingPhaseFailureObjects = new WeakHashMap<>();
  private Map<Object, Object> processedObjects = new WeakHashMap<>();

  private final String initialPhase;
  private final String finalPhase;
  private final Class initialPhaseLifecycleClass;

  /**
   * Creates a new instance.
   * 
   * @param initialPhase the phase to track for execution failures.
   * @param finalPhase the phase to prevent if the {@code trackingPhase} finished with errors.
   * @param initialPhaseLifecycleClass the {@code trackingPhase} interface.
   */
  public DefaultLifecycleInterceptor(String initialPhase, String finalPhase, Class initialPhaseLifecycleClass) {
    this.initialPhase = initialPhase;
    this.finalPhase = finalPhase;
    this.initialPhaseLifecycleClass = initialPhaseLifecycleClass;
  }

  /**
   * Determines if the phase should be skipped if the phase name is {@code phaseToPreventOnTrackingPhaseError} and the object
   * supports the phase {@code phaseToPreventOnTrackingPhaseError} and the phase {@code trackingPhase} phase was not successfully
   * completed
   * <p>
   * If all three of the above conditions are met, then this method returns {@code false}. Otherwise, it returns {@code true}
   *
   * @param phase the phase being applied
   * @param object the target object
   * @return whether the {@code phase} should be applied on the {@code object}
   */
  @Override
  public boolean beforePhaseExecution(LifecyclePhase phase, Object object) {
    if (isFinalPhase(phase) && (initialPhaseLifecycleClass.isAssignableFrom(object.getClass()))) {
      if (trackingPhaseFailureObjects.containsKey(object)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(String.format("Skipping %s lifecycle phase on object because %s phase failed before it could be applied "
              + "on it. Object is: %s", finalPhase, initialPhase, object));
        }
        return false;
      }
    }
    if (isFinalPhase(phase) && (initialPhaseLifecycleClass.isAssignableFrom(object.getClass()))) {
      return processedObjects.containsKey(object);
    }
    processedObjects.put(object, object);
    return true;
  }

  /**
   * If the {@code phase} name is {@code trackingPhase} then it tracks the given {@code object} as successful on the
   * {@code trackingPhase}.
   *
   * @param phase the phase that was applied
   * @param object the target object
   * @param exceptionThrownOptional an optional with an exception. If present then there was an error execution the phase,
   *        otherwise the phase execution was successful
   */
  @Override
  public void afterPhaseExecution(LifecyclePhase phase, Object object, Optional<Exception> exceptionThrownOptional) {
    if (isTrackingPhase(phase) && exceptionThrownOptional.isPresent()) {
      trackingPhaseFailureObjects.put(object, object);
    }
  }

  /**
   * If the {@code phase} name is {@code trackingPhase} then it marks the {@code trackingPhase} phase as completed and clears all
   * other tracking state so that memory can be reclaimed.
   *
   * @param phase the phase that was applied
   */
  @Override
  public void onPhaseCompleted(LifecyclePhase phase) {
    if (isTrackingPhase(phase)) {
      trackingPhaseFailureObjects.clear();
    }
  }

  private boolean isTrackingPhase(LifecyclePhase phase) {
    return initialPhase.equals(phase.getName());
  }

  private boolean isFinalPhase(LifecyclePhase phase) {
    return finalPhase.equals(phase.getName());
  }

  public static LifecycleInterceptor createInitDisposeLifecycleInterceptor() {
    return new DefaultLifecycleInterceptor(Initialisable.PHASE_NAME, Disposable.PHASE_NAME, Initialisable.class);
  }

  public static LifecycleInterceptor createStartStopLifecycleInterceptor() {
    return new DefaultLifecycleInterceptor(Startable.PHASE_NAME, Stoppable.PHASE_NAME, Startable.class);
  }
}
