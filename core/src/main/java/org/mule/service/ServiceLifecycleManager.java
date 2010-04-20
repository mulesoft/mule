/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecyclePair;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ServiceNotification;
import org.mule.lifecycle.AbstractLifecycleManager;
import org.mule.lifecycle.DefaultLifecyclePair;
import org.mule.lifecycle.DefaultLifecyclePhase;
import org.mule.util.StringMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The lifecycle manager responsible for managing lifecycle transitions for a Mule service.  The Mule service adds some additional
 * states, namely pause and resume.  The lifecycle manager manages lifecycle notifications and logging as well.
 */
public class ServiceLifecycleManager extends AbstractLifecycleManager
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ServiceLifecycleManager.class);

    private AbstractService service;

    public ServiceLifecycleManager(AbstractService service, LifecycleManager lifecycleManager) throws MuleException
    {
        this.service = service;
        for (LifecyclePair pair : lifecycleManager.getLifecyclePairs())
        {
            registerLifecycle(pair);
        }
        registerLifecycle(new DefaultLifecyclePair(new PausePhase(), new ResumePhase()));
    }

    /**
     * This lifecycle manager handles calling of lifecycle menthods explicitly since we need to handle custom pause and resume states
     * This method simply checks that the phase is valid and delegates to {@link #invokePhase(org.mule.api.lifecycle.LifecyclePhase)}
     * which in turn calls {@link #doApplyPhase(org.mule.api.lifecycle.LifecyclePhase)}
     * @param phase the phase to transition to
     * @throws LifecycleException if there is an exception thrown when call a lifecycle method
     */
    @Override
    public void fireLifecycle(String phase) throws LifecycleException
    {
        checkPhase(phase);
        LifecyclePhase li = getPhaseForIndex(getPhaseIndex(phase));
        invokePhase(li);
    }

    @Override
    protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
    {
        try
        {
            if(phase.getName().equals(Initialisable.PHASE_NAME))
            {
                logger.debug("Initialising service: " + service.getName());
                service.doInitialise();
                fireServiceNotification(ServiceNotification.SERVICE_INITIALISED);                
            }
            else if(phase.getName().equals(Startable.PHASE_NAME))
            {
                logger.debug("Starting service: " + service.getName());
                service.doStart();
                fireServiceNotification(ServiceNotification.SERVICE_STARTED);
            }
            else if(phase.getName().equals(Pausable.PHASE_NAME))
            {
                logger.debug("Pausing service: " + service.getName());
                service.doPause();
                fireServiceNotification(ServiceNotification.SERVICE_PAUSED);
            }
            else if(phase.getName().equals(Resumable.PHASE_NAME))
            {
                logger.debug("Resuming service: " + service.getName());
                service.doResume();
                fireServiceNotification(ServiceNotification.SERVICE_RESUMED);
            }
            else if(phase.getName().equals(Stoppable.PHASE_NAME))
            {
                logger.debug("Stopping service: " + service.getName());
                service.doStop();
                fireServiceNotification(ServiceNotification.SERVICE_STOPPED);
            }
            else if(phase.getName().equals(Disposable.PHASE_NAME))
            {
                //We need to handle transitions to get to dispose since, dispose can be called from any lifecycle state
                logger.debug("Disposing service: " + service.getName());

                if(getState().isPhaseComplete(Pausable.PHASE_NAME))
                {
                    //This is a work around to bypass the phase checking so that we can call resume even though dispose was called
                    setExecutingPhase(null);                    
                    service.resume();
                }

                if(getState().isStarted())
                {
                    //This is a work around to bypass the phase checking so that we can call stop even though dispose was called
                    setExecutingPhase(null);
                    service.stop();
                }
                service.doDispose();
                fireServiceNotification(ServiceNotification.SERVICE_DISPOSED);
            }
            else
            {
                throw new LifecycleException(CoreMessages.lifecyclePhaseNotRecognised(phase.getName()), service);
            }
        }
        catch (MuleException e)
        {
            throw new LifecycleException(e, service);
        }
    }

    protected void fireServiceNotification(int action)
    {
        service.getMuleContext().fireNotification(new ServiceNotification(service, action));
    }
}
