/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
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
    public static final int MESSAGE_DISPATCH_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 2;
    public static final int MESSAGE_SEND_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 3;
    public static final int MESSAGE_REQUEST_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 4;
    public static final int MESSAGE_RESPONSE = MESSAGE_EVENT_ACTION_START_RANGE + 5;

    public static final int MESSAGE_DISPATCH_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 1;
    public static final int MESSAGE_SEND_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 2;
    public static final int MESSAGE_REQUEST_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 3;

    /**
     * For backwards compatibility.  BEGIN is chosen where it contains the message sent, and END where it contains the message
     * received, again for backwards compatibility.
     */
    public static final int MESSAGE_DISPATCHED = MESSAGE_DISPATCH_BEGIN;
    public static final int MESSAGE_SENT = MESSAGE_SEND_BEGIN;
    public static final int MESSAGE_REQUESTED = MESSAGE_REQUEST_END;


    static
    {
        registerAction("receive", MESSAGE_RECEIVED);
        registerAction("response", MESSAGE_RESPONSE);

        registerAction("begin dispatch", MESSAGE_DISPATCH_BEGIN);
        registerAction("begin send", MESSAGE_SEND_BEGIN);
        registerAction("begin request", MESSAGE_REQUEST_BEGIN);

        registerAction("end dispatch", MESSAGE_DISPATCH_END);
        registerAction("end send", MESSAGE_SEND_END);
        registerAction("end request", MESSAGE_REQUEST_END);
    }

    private String endpoint;
    private ImmutableEndpoint immutableEndpoint;
    private FlowConstruct flowConstruct;

    public EndpointMessageNotification(MuleMessage resource,
                               ImmutableEndpoint endpoint,
                               FlowConstruct flowConstruct,
                               int action)
    {
        super(cloneMessage(resource), action);
        resourceIdentifier = flowConstruct != null ? flowConstruct.getName() : null;
        this.endpoint = endpoint.getEndpointURI().toString();
        this.immutableEndpoint = endpoint;
        this.flowConstruct = flowConstruct;
    }


    @Override
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

    @Override
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

    public ImmutableEndpoint getImmutableEndpoint()
    {
        return immutableEndpoint;
    }

    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    @Override
    public String getType()
    {
        return TYPE_TRACE;
    }

    @Override
    public MuleMessage getSource()
    {
        return (MuleMessage) super.getSource();
    }
}
