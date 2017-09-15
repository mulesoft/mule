/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
