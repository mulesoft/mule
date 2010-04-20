/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecyclePair;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.ServiceNotification;
import org.mule.lifecycle.AbstractLifecycleManager;
import org.mule.service.Pausable;
import org.mule.service.Resumable;

/**
 * TODO
 */
public class ConnectorLifecycleManager extends AbstractLifecycleManager
{
    private AbstractConnector connector;

    public ConnectorLifecycleManager(AbstractConnector connector, LifecycleManager lifecycleManager) throws MuleException
    {
        this.connector = connector;
        //TODO see if we can incorporate connect and disconnect
        for (LifecyclePair pair : lifecycleManager.getLifecyclePairs())
        {
            registerLifecycle(pair);
        }
    }

    /**
     * This lifecycle manager handles calling of lifecycle methods explicitly
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
                logger.debug("Initialising connector: " + connector.getName());
                connector.doInitialise();

            }
            else if(phase.getName().equals(Startable.PHASE_NAME))
            {
                logger.debug("Starting connector: " + connector.getName());
                connector.doStart();
            }

            else if(phase.getName().equals(Stoppable.PHASE_NAME))
            {
                logger.debug("Stopping connector: " + connector.getName());
                connector.doStop();
            }
            else if(phase.getName().equals(Disposable.PHASE_NAME))
            {
                //We need to handle transitions to get to dispose since, dispose can be called from any lifecycle state
                logger.debug("Disposing connector: " + connector.getName());


                if(getState().isStarted())
                {
                    //This is a work around to bypass the phase checking so that we can call stop even though dispose was called
                    setExecutingPhase(null);
                    connector.stop();
                }
                connector.doDispose();
            }
            else
            {
                throw new LifecycleException(CoreMessages.lifecyclePhaseNotRecognised(phase.getName()), connector);
            }
        }
        catch (MuleException e)
        {
            throw new LifecycleException(e, connector);
        }
    }

    protected void fireConnectionNotification(int action)
    {
        connector.getMuleContext().fireNotification(new ConnectionNotification(connector, connector.getName(), action));
    }
}
