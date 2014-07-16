/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
