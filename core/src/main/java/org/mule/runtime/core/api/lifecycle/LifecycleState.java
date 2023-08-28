/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.api.annotation.NoImplement;

/**
 * A safe facade for lifecycle manager that objects can use to monitor its own state
 *
 * @since 3.0
 */
@NoImplement
public interface LifecycleState {

  boolean isInitialised();

  boolean isInitialising();

  boolean isStarted();

  boolean isStarting();

  boolean isStopped();

  boolean isStopping();

  boolean isDisposed();

  boolean isDisposing();

  boolean isPhaseComplete(String phase);

  boolean isPhaseExecuting(String phase);

  boolean isValidTransition(String phase);

}
