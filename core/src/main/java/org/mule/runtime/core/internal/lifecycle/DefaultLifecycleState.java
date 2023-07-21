/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;

/**
 * A safe facade for lifecycle manager that objects can use to monitor its own state
 */
public class DefaultLifecycleState implements LifecycleState {

  private LifecycleManager lifecycleManager;

  public DefaultLifecycleState(LifecycleManager lifecycleManager) {
    this.lifecycleManager = lifecycleManager;
  }

  public boolean isInitialised() {
    return lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME);
  }

  public boolean isInitialising() {
    return Initialisable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
  }

  public boolean isStarted() {
    return Startable.PHASE_NAME.equals(lifecycleManager.getCurrentPhase());
  }

  public boolean isStarting() {
    return Startable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
  }

  public boolean isStopped() {
    return Stoppable.PHASE_NAME.equals(lifecycleManager.getCurrentPhase());
  }

  public boolean isStopping() {
    return Stoppable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
  }

  public boolean isDisposed() {
    return lifecycleManager.isPhaseComplete(Disposable.PHASE_NAME);
  }

  public boolean isDisposing() {
    return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
  }

  public boolean isPhaseComplete(String phase) {
    return lifecycleManager.isPhaseComplete(phase);
  }

  public boolean isPhaseExecuting(String phase) {
    String executingPhase = lifecycleManager.getExecutingPhase();
    if (executingPhase != null) {
      return executingPhase.equals(phase);
    }
    return false;
  }

  public boolean isValidTransition(String phase) {
    return lifecycleManager.isDirectTransition(phase);
  }
}
