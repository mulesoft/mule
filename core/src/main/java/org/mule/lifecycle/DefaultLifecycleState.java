/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * A safe facade for lifecycle manager that objects can use to monitor its own state
 */
public class DefaultLifecycleState implements LifecycleState
{
    private LifecycleManager lifecycleManager;

    DefaultLifecycleState(LifecycleManager lifecycleManager)
    {
        this.lifecycleManager = lifecycleManager;
    }

    public boolean isInitialised()
    {
        return lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME);
    }

    public boolean isInitialising()
    {
        return Initialisable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isStarted()
    {
        return Startable.PHASE_NAME.equals(lifecycleManager.getCurrentPhase());
    }

    public boolean isStarting()
    {
        return Startable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isStopped()
    {
        return Stoppable.PHASE_NAME.equals(lifecycleManager.getCurrentPhase());
    }

    public boolean isStopping()
    {
        return Stoppable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isDisposed()
    {
        return lifecycleManager.isPhaseComplete(Disposable.PHASE_NAME);
    }

    public boolean isDisposing()
    {
        return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isPhaseComplete(String phase)
    {
        return lifecycleManager.isPhaseComplete(phase);
    }

    public boolean isPhaseExecuting(String phase)
    {
        String executingPhase = lifecycleManager.getExecutingPhase();
        if(executingPhase!=null)
        {
            return executingPhase.equals(phase);
        }
        return false;
    }

    public boolean isValidTransition(String phase)
    {
        return lifecycleManager.isDirectTransition(phase);
    }
}
