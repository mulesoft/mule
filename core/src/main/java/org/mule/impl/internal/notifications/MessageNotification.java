/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.impl.MuleMessage;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.provider.UMOConnectable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * These notifications are fire when either a message is received via an endpoint, or
 * dispatcher of if a receive call is made on a dispatcher.
 */
public class MessageNotification extends UMOServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5118299601117624094L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MessageNotification.class);

    public static final int MESSAGE_RECEIVED = MESSAGE_EVENT_ACTION_START_RANGE + 1;
    public static final int MESSAGE_DISPATCHED = MESSAGE_EVENT_ACTION_START_RANGE + 2;
    public static final int MESSAGE_SENT = MESSAGE_EVENT_ACTION_START_RANGE + 3;
    public static final int MESSAGE_REQUESTED = MESSAGE_EVENT_ACTION_START_RANGE + 4;

    static {
        registerAction("received", MESSAGE_RECEIVED);
        registerAction("dispatched", MESSAGE_DISPATCHED);
        registerAction("sent", MESSAGE_SENT);
        registerAction("requested", MESSAGE_REQUESTED);
    }

    private UMOImmutableEndpoint endpoint;

    public MessageNotification(UMOMessage resource,
                               UMOImmutableEndpoint endpoint,
                               String identifier,
                               int action)
    {
        super(cloneMessage(resource), action);
        resourceIdentifier = identifier;
        this.endpoint = endpoint;

    }

    protected static UMOMessage cloneMessage(UMOMessage message)
    {
        // TODO we probably need to support deep cloning here
        synchronized (message)
        {
            return new MuleMessage(message.getPayload(), message);
        }
    }

    protected String getPayloadToString()
    {
        if (source instanceof UMOConnectable)
        {
            return ((UMOConnectable) source).getConnectionDescription();
        }
        return source.toString();
    }

    public String toString()
    {
        return EVENT_NAME + "{action=" + getActionName(action) + ", endpoint: " + endpoint.getEndpointURI()
                        + ", resourceId=" + resourceIdentifier + ", timestamp=" + timestamp + ", serverId="
                        + serverId + ", message: " + source + "}";
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

}
