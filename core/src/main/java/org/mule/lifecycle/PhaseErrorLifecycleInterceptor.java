/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import java.util.Map;
import java.util.WeakHashMap;

import org.mule.api.lifecycle.LifecycleInterceptor;
import org.mule.api.lifecycle.LifecyclePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * A {@link LifecycleInterceptor} which tracks the completion of the a phase so that if it fails to complete, the another phase is
 * only applied on the target objects on which it could be successfully applied.
 *
 * @since 3.8
 */
public class PhaseErrorLifecycleInterceptor implements LifecycleInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(PhaseErrorLifecycleInterceptor.class);

  private Map<Object, Object> trackingPhaseFailureObjects = new WeakHashMap<>();

  private final String trackingPhase;
  private final String phaseToPreventOnTrackingPhaseError;
  private final Class trackingPhaseClass;

  /**
   * Creates a new instance.
   * 
   * @param trackingPhase the phase to track for execution failures.
   * @param phaseToPreventOnTrackingPhaseError the phase to prevent if the {@code trackingPhase} finished with errors.
   * @param lifecycleClass the {@code trackingPhase} interface.
   */
  public PhaseErrorLifecycleInterceptor(String trackingPhase, String phaseToPreventOnTrackingPhaseError, Class lifecycleClass) {
    this.trackingPhase = trackingPhase;
    this.phaseToPreventOnTrackingPhaseError = phaseToPreventOnTrackingPhaseError;
    this.trackingPhaseClass = lifecycleClass;
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
    if (isPhaseToPrevent(phase) && (trackingPhaseClass.isAssignableFrom(object.getClass()))) {
      if (trackingPhaseFailureObjects.containsKey(object)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(String.format("Skipping %s lifecycle phase on object because %s phase failed before it could be applied "
              + "on it. Object is: %s", phaseToPreventOnTrackingPhaseError, trackingPhase, object));
        }
        return false;
      }
    }
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
    return trackingPhase.equals(phase.getName());
  }

  private boolean isPhaseToPrevent(LifecyclePhase phase) {
    return phaseToPreventOnTrackingPhaseError.equals(phase.getName());
  }
}