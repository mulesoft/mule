/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.i18n.CoreMessages;
import org.mule.lifecycle.SimpleLifecycleManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The lifecycle manager responsible for managing lifecycle transitions for a Mule
 * service. The Mule service adds some additional states, namely pause and resume.
 * The lifecycle manager manages lifecycle notifications and logging as well.
 */
public class ComponentLifecycleManager extends SimpleLifecycleManager<Component>
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ComponentLifecycleManager.class);
    protected MuleContext muleContext;

    public ComponentLifecycleManager(String name, Component component)
    {
        super(name, component);
    }

    @Override
    public void fireInitialisePhase(LifecycleCallback<Component> callback) throws InitialisationException
    {
        checkPhase(Initialisable.PHASE_NAME);
        if (logger.isInfoEnabled()) logger.info("Initialising component: " + lifecycleManagerId);
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
            throw new InitialisationException(e, (Initialisable) object);
        }
    }

    @Override
    public void fireStartPhase(LifecycleCallback<Component> callback) throws MuleException
    {
        checkPhase(Startable.PHASE_NAME);
        if (logger.isInfoEnabled()) logger.info("Starting component: " + lifecycleManagerId);
        invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireStopPhase(LifecycleCallback<Component> callback) throws MuleException
    {
        checkPhase(Stoppable.PHASE_NAME);
        if (logger.isInfoEnabled()) logger.info("Stopping component: " + lifecycleManagerId);
        invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireDisposePhase(LifecycleCallback<Component> callback)
    {
        checkPhase(Disposable.PHASE_NAME);
        if (logger.isInfoEnabled()) logger.info("Disposing component: " + lifecycleManagerId);
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
