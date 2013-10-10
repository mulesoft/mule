/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.registry.AbstractRegistryBroker;

public class RegistryBrokerLifecycleManager extends RegistryLifecycleManager
{

    public RegistryBrokerLifecycleManager(String id, Registry object, MuleContext muleContext)
    {
        super(id, object, muleContext);
    }

    @Override
    protected void registerPhases()
    {
        RegistryLifecycleCallback callback = new RegistryLifecycleCallback();
        registerPhase(Initialisable.PHASE_NAME, new MuleContextInitialisePhase(),
            new EmptyLifecycleCallback<AbstractRegistryBroker>());
        registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), callback);
        registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), callback);
        registerPhase(Disposable.PHASE_NAME, new MuleContextDisposePhase(),
            new EmptyLifecycleCallback<AbstractRegistryBroker>());
    }

    public void fireInitialisePhase(LifecycleCallback<AbstractRegistryBroker> callback)
        throws InitialisationException
    {
        checkPhase(Initialisable.PHASE_NAME);

        if (logger.isInfoEnabled())
        {
            logger.info("Initialising RegistryBroker");
        }

        // No pre notification
        try
        {
            invokePhase(Initialisable.PHASE_NAME, getLifecycleObject(), callback);
        }
        catch (LifecycleException e)
        {
            throw new InitialisationException(e, object);
        }
        // No post notification
    }

    public void fireDisposePhase(LifecycleCallback<AbstractRegistryBroker> callback)
    {
        checkPhase(Disposable.PHASE_NAME);

        if (logger.isInfoEnabled())
        {
            logger.info("Disposing RegistryBroker");
        }

        // No pre notification
        try
        {
            invokePhase(Disposable.PHASE_NAME, getLifecycleObject(), callback);
        }
        catch (LifecycleException e)
        {
            logger.error("Failed to shut down registry broker cleanly: ", e);
        }
        // No post notification
    }

}
