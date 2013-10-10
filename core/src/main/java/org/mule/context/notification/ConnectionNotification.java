/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.transport.Connectable;

/**
 * Is fired by a connector when a connection is made or disconnected.
 * A disconnection can be caused by network failure, JMX, or the server shutting down.
 */
public class ConnectionNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6455441938378523145L;
    public static final int CONNECTION_CONNECTED = CONNECTION_EVENT_ACTION_START_RANGE + 1;
    public static final int CONNECTION_FAILED = CONNECTION_EVENT_ACTION_START_RANGE + 2;
    public static final int CONNECTION_DISCONNECTED = CONNECTION_EVENT_ACTION_START_RANGE + 3;

    static {
        registerAction("connected", CONNECTION_CONNECTED);
        registerAction("connect failed", CONNECTION_FAILED);
        registerAction("disconnected", CONNECTION_DISCONNECTED);
    }

    public ConnectionNotification(Connectable resource, String identifier, int action)
    {
        super((resource == null ? identifier : resource.getConnectionDescription()), action);
        resourceIdentifier = identifier;
    }

    @Override
    protected String getPayloadToString()
    {
        return source.toString();
    }

    @Override
    public String getType()
    {
        if (action == CONNECTION_DISCONNECTED)
        {
            return TYPE_WARNING;
        }
        if (action == CONNECTION_FAILED)
        {
            return TYPE_ERROR;
        }
        return TYPE_INFO;
    }

}
