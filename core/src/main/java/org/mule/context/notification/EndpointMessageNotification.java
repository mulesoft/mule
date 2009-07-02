/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.ImmutableEndpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * These notifications are fired when either a message is either: received by an
 * endpoint, sent or dispatched from an endpoint or requested from an endpoint.
 */
public class EndpointMessageNotification extends ServerNotification
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5118299601117624094L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(EndpointMessageNotification.class);

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

    private String endpoint;

    public EndpointMessageNotification(MuleMessage resource,
                               ImmutableEndpoint endpoint,
                               String identifier,
                               int action)
    {
        super(cloneMessage(resource), action);
        resourceIdentifier = identifier;
        this.endpoint = endpoint.getEndpointURI().toString();
    }


    protected String getPayloadToString()
    {
        try
        {
            return ((MuleMessage)source).getPayloadAsString();
        }
        catch (Exception e)
        {
            return source.toString();
        }
    }

    public String toString()
    {
        return EVENT_NAME + "{action=" + getActionName(action) + ", endpoint: " + endpoint
                        + ", resourceId=" + resourceIdentifier + ", timestamp=" + timestamp + ", serverId="
                        + serverId + ", message: " + source + "}";
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public String getType()
    {
        return TYPE_TRACE;
    }

}
