/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

/**
 * A safe facade for lifecycle manager that objects can use to monitor its own state
 *
 * @since 3.0
 */
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
