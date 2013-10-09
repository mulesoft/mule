/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
