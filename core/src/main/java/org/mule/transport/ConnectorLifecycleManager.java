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
import org.mule.api.transport.Connector;
import org.mule.context.notification.ConnectionNotification;
import org.mule.lifecycle.SimpleLifecycleManager;

/**
 * Manages the lifecycle of connectors in Mule. Currently only manages the
 * 'initialsie', 'start', 'stop' and 'dispose' phases, not the connect phase which is
 * managed by the Retry handler
 *
 * @since 3.0
 */
public class ConnectorLifecycleManager extends SimpleLifecycleManager<Connector>
{
    public ConnectorLifecycleManager(AbstractConnector connector)
    {
        super(connector.getName(), connector);
    }

    @Override
    public void fireInitialisePhase(LifecycleCallback<Connector> callback) throws MuleException
    {
        checkPhase(Initialisable.PHASE_NAME);

        if (logger.isInfoEnabled())
        {
            logger.info("Initialising connector: " + getLifecycleObject().getName());
        }

        // No pre notification
        invokePhase(Initialisable.PHASE_NAME, getLifecycleObject(), callback);
        // No post notification
    }

    @Override
    public void fireStartPhase(LifecycleCallback<Connector> callback) throws MuleException
    {
        checkPhase(Startable.PHASE_NAME);

        if (logger.isInfoEnabled())
        {
            logger.info("Starting connector: " + getLifecycleObject().getName());
        }

        // No pre notification
        invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
        // No post notification
    }

    @Override
    public void fireStopPhase(LifecycleCallback<Connector> callback) throws MuleException
    {
        checkPhase(Stoppable.PHASE_NAME);
        if (logger.isInfoEnabled())

        {
            logger.info("Stopping connector: " + getLifecycleObject().getName());
        }

        // No pre notification
        invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
        // No post notification
    }

    @Override
    public void fireDisposePhase(LifecycleCallback<Connector> callback) throws MuleException
    {
        checkPhase(Disposable.PHASE_NAME);

        if (logger.isInfoEnabled())
        {
            logger.info("Disposing connector: " + getLifecycleObject().getName());
        }

        // No pre notification
        invokePhase(Disposable.PHASE_NAME, getLifecycleObject(), callback);
        // No post notification
    }

    protected void fireNotification(int action)
    {
        getLifecycleObject().getMuleContext().fireNotification(
            new ConnectionNotification(getLifecycleObject(), getLifecycleObject().getName(), action));
    }
}
