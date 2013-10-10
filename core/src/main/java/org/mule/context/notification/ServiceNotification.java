/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.service.Service;

/**
 * <code>ServiceNotification</code> is fired when an event such as the service
 * starting occurs. The payload of this event will always be a reference to the
 * service.
 */
public class ServiceNotification extends ServerNotification implements BlockingServerEvent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8575741816897936674L;
    public static final int SERVICE_INITIALISED = SERVICE_EVENT_ACTION_START_RANGE + 1;
    public static final int SERVICE_STARTED = SERVICE_EVENT_ACTION_START_RANGE + 2;
    public static final int SERVICE_STOPPED = SERVICE_EVENT_ACTION_START_RANGE + 3;
    public static final int SERVICE_PAUSED = SERVICE_EVENT_ACTION_START_RANGE + 4;
    public static final int SERVICE_RESUMED = SERVICE_EVENT_ACTION_START_RANGE + 5;
    public static final int SERVICE_DISPOSED = SERVICE_EVENT_ACTION_START_RANGE + 6;

    static {
        registerAction("service initialised", SERVICE_INITIALISED);
        registerAction("service started", SERVICE_STARTED);
        registerAction("service stopped", SERVICE_STOPPED);
        registerAction("service paused", SERVICE_PAUSED);
        registerAction("service resumed", SERVICE_RESUMED);
        registerAction("service disposed", SERVICE_DISPOSED);
    }

    public ServiceNotification(Service message, int action)
    {
        super(message.getName(), action);
        resourceIdentifier = message.getName();
    }
}
