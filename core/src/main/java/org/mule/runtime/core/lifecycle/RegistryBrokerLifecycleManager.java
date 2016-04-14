/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
import org.mule.api.registry.RegistryBroker;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.registry.AbstractRegistryBroker;

/**
 * @deprecated as of 3.7.0 since {@link RegistryBroker} also is
 */
@Deprecated
public class RegistryBrokerLifecycleManager extends RegistryLifecycleManager
{

    public RegistryBrokerLifecycleManager(String id, Registry object, MuleContext muleContext)
    {
        super(id, object, muleContext);
    }

    @Override
    protected void registerPhases()
    {
        RegistryLifecycleCallback callback = new RegistryLifecycleCallback(this);
        LifecycleCallback<AbstractRegistryBroker> emptyCallback = new EmptyLifecycleCallback<>();
        registerPhase(Initialisable.PHASE_NAME, new MuleContextInitialisePhase(), emptyCallback);
        registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), callback);
        registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), callback);
        registerPhase(Disposable.PHASE_NAME, new MuleContextDisposePhase(), emptyCallback);
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
