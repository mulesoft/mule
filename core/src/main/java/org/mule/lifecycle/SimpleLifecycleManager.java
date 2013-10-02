/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.lifecycle.phases.NotInLifecyclePhase;

/**
 * This {@link org.mule.api.lifecycle.LifecycleManager} implementation is designed to track the lifecycle of objects
 * that support the {@link org.mule.api.lifecycle.Initialisable#PHASE_NAME}, {@link org.mule.api.lifecycle.Startable#PHASE_NAME},
 * {@link org.mule.api.lifecycle.Stoppable#PHASE_NAME} and {@link org.mule.api.lifecycle.Disposable#PHASE_NAME} phases and
 * adds convenience methods for firing these phases by callbacks.  
 * 
 * This is an internal class used by Mule for managing state for objects such as {@link org.mule.api.service.Service},
 * {@link org.mule.api.transport.Connector} and {@link org.mule.api.agent.Agent}, all of which can be controlled externally via JMX
 * @param <O> the object type being managed by this {@link org.mule.api.lifecycle.LifecycleManager}
 */
public abstract class SimpleLifecycleManager<O> extends AbstractLifecycleManager<O>
{
    public SimpleLifecycleManager(String id, O object)
    {
        super(id, object);
    }

    @Override
    protected void registerTransitions()
    {
        //init dispose
        addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
        addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Disposable.PHASE_NAME);
        addDirectTransition(Initialisable.PHASE_NAME, Startable.PHASE_NAME);

        //If an object fails to start, the object can be left in an initialise state, but the container can be started
        addDirectTransition(Initialisable.PHASE_NAME, Stoppable.PHASE_NAME);
        addDirectTransition(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);

        //start stop
        addDirectTransition(Startable.PHASE_NAME, Stoppable.PHASE_NAME);
        addDirectTransition(Stoppable.PHASE_NAME, Startable.PHASE_NAME);
        addDirectTransition(Stoppable.PHASE_NAME, Disposable.PHASE_NAME);
    }

    public void fireLifecycle(String phase) throws LifecycleException
    {
        throw new UnsupportedOperationException("SimpleLifecycleManager.fireLifecycle");
    }

    public abstract void fireInitialisePhase(LifecycleCallback<O> callback) throws MuleException;

    public abstract void fireStartPhase(LifecycleCallback<O> callback) throws MuleException;

    public abstract void fireStopPhase(LifecycleCallback<O> callback) throws MuleException;

    public abstract void fireDisposePhase(LifecycleCallback<O> callback) throws MuleException;
}
