/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.core.internal.lifecycle.phases.LifecyclePhase;

import java.util.Optional;

/**
 * Implementation of the {@code null object} design pattern for the {@link LifecycleInterceptor} interface
 *
 * @since 3.8
 */
public final class NullLifecycleInterceptor implements LifecycleInterceptor {

  /**
   * @return always returns {@code true}
   */
  @Override
  public boolean beforePhaseExecution(LifecyclePhase phase, Object object) {
    return true;
  }

  /**
   * NoOp implementation
   */
  @Override
  public void afterPhaseExecution(LifecyclePhase phase, Object object, Optional<Exception> exceptionThrownOptional) {

  }

  /**
   * NoOp implementation
   */
  @Override
  public void onPhaseCompleted(LifecyclePhase phase) {

  }
}
