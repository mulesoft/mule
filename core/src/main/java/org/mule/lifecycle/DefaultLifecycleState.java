/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        return lifecycleManager.isPhaseComplete(Startable.PHASE_NAME);
    }

    public boolean isStarting()
    {
        return Startable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isStopped()
    {
        return lifecycleManager.isPhaseComplete(Stoppable.PHASE_NAME);
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
}
