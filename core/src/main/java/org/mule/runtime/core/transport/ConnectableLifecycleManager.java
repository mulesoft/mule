/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.lifecycle.SimpleLifecycleManager;

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
