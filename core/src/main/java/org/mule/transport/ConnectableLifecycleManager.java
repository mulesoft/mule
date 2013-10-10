/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.lifecycle.SimpleLifecycleManager;

/**
 * TODO
 */
public class ConnectableLifecycleManager<O> extends SimpleLifecycleManager<O>
{
    public ConnectableLifecycleManager(String id, O object)
    {
        super(id, object);
    }

    @Override
    public void fireInitialisePhase(LifecycleCallback<O> callback) throws MuleException
    {
        checkPhase(Initialisable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info(String.format("Initialising: '%s'. Object is: %s", lifecycleManagerId,
                getLifecycleObject().getClass().getSimpleName()));
        }
        invokePhase(Initialisable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireStartPhase(LifecycleCallback<O> callback) throws MuleException
    {
        checkPhase(Startable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info(String.format("Starting: '%s'. Object is: %s", lifecycleManagerId,
                getLifecycleObject().getClass().getSimpleName()));
        }
        invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireStopPhase(LifecycleCallback<O> callback) throws MuleException
    {
        // We are sometimes stopped by our owner when already stopped
        if (currentPhase.equals(Stoppable.PHASE_NAME))
        {
            return;
        }
        checkPhase(Stoppable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info(String.format("Stopping: '%s'. Object is: %s", lifecycleManagerId,
                getLifecycleObject().getClass().getSimpleName()));
        }
        invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireDisposePhase(LifecycleCallback<O> callback) throws MuleException
    {
        checkPhase(Disposable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info(String.format("Disposing: '%s'. Object is: %s", lifecycleManagerId,
                getLifecycleObject().getClass().getSimpleName()));
        }
        invokePhase(Disposable.PHASE_NAME, getLifecycleObject(), callback);
    }
}
