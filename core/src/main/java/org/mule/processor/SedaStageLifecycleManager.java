/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.i18n.CoreMessages;
import org.mule.lifecycle.SimpleLifecycleManager;
import org.mule.service.Pausable;
import org.mule.service.Resumable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The lifecycle manager responsible for managing lifecycle transitions for a Mule service.  The Mule service adds some additional
 * states, namely pause and resume.  The lifecycle manager manages lifecycle notifications and logging as well.
 */
public class SedaStageLifecycleManager extends SimpleLifecycleManager<SedaStageInterceptingMessageProcessor>
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(SedaStageLifecycleManager.class);

    public SedaStageLifecycleManager(String name, SedaStageInterceptingMessageProcessor sedaStage)
    {
        super(name, sedaStage);
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
    protected void notifyTransition(String destinationPhase)
    {
        if (destinationPhase.equals(Resumable.PHASE_NAME))
        {
            //Revert back to start phase
            completedPhases.remove(Resumable.PHASE_NAME);
            completedPhases.remove(Pausable.PHASE_NAME);
            setCurrentPhase(Startable.PHASE_NAME);
        }
    }

    @Override
    public void fireInitialisePhase(LifecycleCallback<SedaStageInterceptingMessageProcessor> callback) throws InitialisationException
    {
        checkPhase(Initialisable.PHASE_NAME);
        if(logger.isInfoEnabled()) logger.info("Initialising service: " + lifecycleManagerId);
        try
        {
            invokePhase(Initialisable.PHASE_NAME, getLifecycleObject(), callback);
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (LifecycleException e)
        {
            throw new InitialisationException(e, object);
        }
    }

    @Override
    public void fireStartPhase(LifecycleCallback<SedaStageInterceptingMessageProcessor> callback) throws MuleException
    {
        checkPhase(Startable.PHASE_NAME);
        if(logger.isInfoEnabled()) logger.info("Starting service: " + lifecycleManagerId);
        invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
    }

    public void firePausePhase(LifecycleCallback<SedaStageInterceptingMessageProcessor> callback) throws MuleException
    {
        checkPhase(Pausable.PHASE_NAME);
        if(logger.isInfoEnabled()) logger.info("Pausing service: " + lifecycleManagerId);
        invokePhase(Pausable.PHASE_NAME, getLifecycleObject(), callback);
    }

    public void fireResumePhase(LifecycleCallback<SedaStageInterceptingMessageProcessor> callback) throws MuleException
    {
        checkPhase(Resumable.PHASE_NAME);
        if(logger.isInfoEnabled()) logger.info("Resuming service: " + lifecycleManagerId);
        invokePhase(Resumable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireStopPhase(LifecycleCallback<SedaStageInterceptingMessageProcessor> callback) throws MuleException
    {
        checkPhase(Stoppable.PHASE_NAME);
        if(logger.isInfoEnabled()) logger.info("Stopping service: " + lifecycleManagerId);
        invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireDisposePhase(LifecycleCallback<SedaStageInterceptingMessageProcessor> callback)
    {
        checkPhase(Disposable.PHASE_NAME);
        if(logger.isInfoEnabled()) logger.info("Disposing service: " + lifecycleManagerId);
        try
        {
            invokePhase(Disposable.PHASE_NAME, getLifecycleObject(), callback);
        }
        catch (LifecycleException e)
        {
            logger.warn(CoreMessages.failedToDispose(lifecycleManagerId), e);
        }
    }

}
