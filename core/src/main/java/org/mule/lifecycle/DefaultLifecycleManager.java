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
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.i18n.CoreMessages;


/**
 * <p>
 * Default implementation of a {@link SimpleLifecycleManager} it allows {@link Lifecycle} objects to manage their
 * lifecycle easily.
 * </p>
 *
 * @param <T> Type of the object that we need to manage the lifecycle
 * @since 3.5.0
 */
public class DefaultLifecycleManager<T extends Lifecycle> extends SimpleLifecycleManager<T>
{


    public DefaultLifecycleManager(String id, T object)
    {
        super(id, object);
    }


    @Override
    public void fireInitialisePhase(LifecycleCallback<T> callback) throws InitialisationException
    {
        checkPhase(Initialisable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Initialising Bean: " + lifecycleManagerId);
        }
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
    public void fireStartPhase(LifecycleCallback<T> callback) throws MuleException
    {
        checkPhase(Startable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Starting Bean: " + lifecycleManagerId);
        }
        invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireStopPhase(LifecycleCallback<T> callback) throws MuleException
    {
        checkPhase(Stoppable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Stopping Bean: " + lifecycleManagerId);
        }
        invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
    }

    @Override
    public void fireDisposePhase(LifecycleCallback<T> callback)
    {
        checkPhase(Disposable.PHASE_NAME);
        if (logger.isInfoEnabled())
        {
            logger.info("Disposing Bean: " + lifecycleManagerId);
        }
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
