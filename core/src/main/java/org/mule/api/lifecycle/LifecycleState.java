/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

/**
 * A safe facade for lifecycle manager that objects can use to monitor its own state
 *
 * @since 3.0
 */
public interface LifecycleState
{
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
