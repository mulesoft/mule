/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.context.notification.FlowConstructNotification;
import org.mule.lifecycle.SimpleLifecycleManager;
import org.mule.service.Pausable;
import org.mule.service.Resumable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The lifecycle manager responsible for managing lifecycle transitions for a Mule service.  The Mule service adds some additional
 * states, namely pause and resume.  The lifecycle manager manages lifecycle notifications and logging as well.
 */
public class FlowConstructLifecycleManager extends SimpleLifecycleManager<FlowConstruct>
{

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(FlowConstructLifecycleManager.class);
    protected MuleContext muleContext;


    public FlowConstructLifecycleManager(FlowConstruct flowConstruct, MuleContext muleContext)
    {
        super(flowConstruct.getName(), flowConstruct);
        this.muleContext = muleContext;
    }

    @Override
    protected void registerTransitions()
    {
        super.registerTransitions();

        //pause resume
        addDirectTransition(Startable.PHASE_NAME, Pausable.PHASE_NAME);
        //Note that 'Resume' state gets removed and the current state is set to 'start'. See {@link #notifyTransition}
        addDirectTransition(Pausable.PHASE_NAME, Resumable.PHASE_NAME);
        addDirectTransition(Pausable.PHASE_NAME, Stoppable.PHASE_NAME);
    }

    @Override
    protected void notifyTransition(String currentPhase)
    {
        if (currentPhase.equals(Resumable.PHASE_NAME))
        {
            //Revert back to start phase
            completedPhases.remove(Resumable.PHASE_NAME);
            completedPhases.remove(Pausable.PHASE_NAME);
            setCurrentPhase(Startable.PHASE_NAME);
        }
    }

    @Override
    public void fireInitialisePhase(LifecycleCallback<FlowConstruct> callback) throws MuleException
    {
        checkPhase(Initialisable.PHASE_NAME);
        //TODO No pre notification
        if (logger.isInfoEnabled())
        {
            logger.info("Initialising flow: " + getLifecycleObject().getName());
        }
        invokePhase(Initialisable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(FlowConstructNotification.FLOW_CONSTRUCT_INITIALISED);
    }


    @Override
    public void fireStartPhase(LifecycleCallback<FlowConstruct> callback) throws MuleException
    {
        checkPhase(Startable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Starting flow: " + getLifecycleObject().getName());
        }
        //TODO No pre notification
        invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(FlowConstructNotification.FLOW_CONSTRUCT_STARTED);
    }


    public void firePausePhase(LifecycleCallback<FlowConstruct> callback) throws MuleException
    {
        checkPhase(Pausable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Pausing flow: " + getLifecycleObject().getName());
        }

        //TODO No pre notification
        invokePhase(Pausable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(FlowConstructNotification.FLOW_CONSTRUCT_PAUSED);
    }

    public void fireResumePhase(LifecycleCallback<FlowConstruct> callback) throws MuleException
    {
        checkPhase(Resumable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Resuming flow: " + getLifecycleObject().getName());
        }
        //TODO No pre notification
        invokePhase(Resumable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(FlowConstructNotification.FLOW_CONSTRUCT_RESUMED);
    }

    @Override
    public void fireStopPhase(LifecycleCallback<FlowConstruct> callback) throws MuleException
    {
        checkPhase(Stoppable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Stopping flow: " + getLifecycleObject().getName());
        }
        //TODO No pre notification
        invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(FlowConstructNotification.FLOW_CONSTRUCT_STOPPED);
    }

    @Override
    public void fireDisposePhase(LifecycleCallback<FlowConstruct> callback) throws MuleException
    {
        checkPhase(Disposable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Disposing flow: " + getLifecycleObject().getName());
        }
        //TODO No pre notification
        invokePhase(Disposable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(FlowConstructNotification.FLOW_CONSTRUCT_DISPOSED);
    }

    protected void fireNotification(int action)
    {
        muleContext.fireNotification(new FlowConstructNotification(getLifecycleObject(), action));
    }
}
