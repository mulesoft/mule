/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
@Deprecated
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
