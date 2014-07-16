/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.context.notification.ModelNotification;
import org.mule.lifecycle.SimpleLifecycleManager;

/**
 * Handles Lifecycle transitions for {@link org.mule.api.model.Model} implementations
 */
@Deprecated
public class ModelLifecycleManager extends SimpleLifecycleManager<AbstractModel>
{
    public ModelLifecycleManager(AbstractModel model)
    {
        super(model.getName(), model);
    }

    @Override
    public void fireInitialisePhase(LifecycleCallback<AbstractModel> callback) throws MuleException
    {
        checkPhase(Initialisable.PHASE_NAME);
        //TODO No pre notification
        if(logger.isInfoEnabled()) logger.info("Initialising model: " + getLifecycleObject().getName());
        invokePhase(Initialisable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(ModelNotification.MODEL_INITIALISED);
    }

    @Override
    public void fireStartPhase(LifecycleCallback<AbstractModel> callback) throws MuleException
    {
        checkPhase(Startable.PHASE_NAME);
        //TODO No pre notification
        if(logger.isInfoEnabled()) logger.info("Starting model: " + getLifecycleObject().getName());
        invokePhase(Startable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(ModelNotification.MODEL_STARTED);
    }

    @Override
    public void fireStopPhase(LifecycleCallback<AbstractModel> callback) throws MuleException
    {
        checkPhase(Stoppable.PHASE_NAME);
        //TODO No pre notification
        if(logger.isInfoEnabled()) logger.info("Stopping model: " + getLifecycleObject().getName());
        invokePhase(Stoppable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(ModelNotification.MODEL_STOPPED);
    }

    @Override
    public void fireDisposePhase(LifecycleCallback<AbstractModel> callback) throws MuleException
    {
        checkPhase(Disposable.PHASE_NAME);
        //TODO No pre notification
        if(logger.isInfoEnabled()) logger.info("Disposing model: " + getLifecycleObject().getName());
        invokePhase(Disposable.PHASE_NAME, getLifecycleObject(), callback);
        fireNotification(ModelNotification.MODEL_DISPOSED);
    }

    void fireNotification(int action)
    {
        getLifecycleObject().getMuleContext().fireNotification(new ModelNotification(getLifecycleObject(), action));
    }
}
