/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.model.Model;

/**
 * <code>ModelNotification</code> is fired when an event such as the model starting
 * occurs. The payload of this event will always be a reference to the model.
 * 
 * @see org.mule.api.model.Model
 */
public class ModelNotification extends ServerNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1954880336427554435L;

    public static final int MODEL_INITIALISED = MODEL_EVENT_ACTION_START_RANGE + 2;
    public static final int MODEL_STARTED = MODEL_EVENT_ACTION_START_RANGE + 6;
    public static final int MODEL_STOPPED = MODEL_EVENT_ACTION_START_RANGE + 8;
    public static final int MODEL_DISPOSED = MODEL_EVENT_ACTION_START_RANGE + 10;

    static {
        registerAction("model initialised", MODEL_INITIALISED);
        registerAction("model started", MODEL_STARTED);
        registerAction("model stopped", MODEL_STOPPED);
        registerAction("model disposed", MODEL_DISPOSED);
    }

    public ModelNotification(Model model, int action)
    {
        super(model.getName(), action);
        resourceIdentifier = model.getName();
    }
}
