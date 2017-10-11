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

import java.util.Optional;

/**
 * Main {@link LifecycleInterceptor} used in mule so lifecycle semantics are properly enforced.
 * <p/>
 * This implementation keeps track of the object
 */
public class MuleLifecycleInterceptor implements LifecycleInterceptor {

  private LifecycleInterceptor initDisposeLifecycleInterceptor =
      new DefaultLifecycleInterceptor(Initialisable.PHASE_NAME, Disposable.PHASE_NAME, Initialisable.class);
  private LifecycleInterceptor startStopLifecycleInterceptor =
      new DefaultLifecycleInterceptor(Startable.PHASE_NAME, Stoppable.PHASE_NAME, Startable.class);

  @Override
  public boolean beforePhaseExecution(LifecyclePhase phase, Object object) {
    return getLifecycleInterceptor(phase).beforePhaseExecution(phase, object);
  }

  private LifecycleInterceptor getLifecycleInterceptor(LifecyclePhase phase) {
    if (phase.getName().equals(Initialisable.PHASE_NAME) || phase.getName().equals(Disposable.PHASE_NAME)) {
      return initDisposeLifecycleInterceptor;
    }
    return startStopLifecycleInterceptor;
  }

  @Override
  public void afterPhaseExecution(LifecyclePhase phase, Object object, Optional<Exception> exceptionThrownOptional) {
    getLifecycleInterceptor(phase).afterPhaseExecution(phase, object, exceptionThrownOptional);
  }

  @Override
  public void onPhaseCompleted(LifecyclePhase phase) {
    getLifecycleInterceptor(phase).onPhaseCompleted(phase);
  }
}
