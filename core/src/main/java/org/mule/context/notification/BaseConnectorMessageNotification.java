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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * These notifications are fired when either a message is received from or dispatched to an external entity.
 * An endpoint based {@link org.mule.api.transport.Connector} (transports) should fire a {@link org.mule.context.notification.EndpointMessageNotification}
 * Non-transport {@link org.mule.api.transport.Connector} should fire a {@link org.mule.context.notification.ConnectorMessageNotification}
 */
public abstract class BaseConnectorMessageNotification extends ServerNotification
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -5118299601117624094L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(BaseConnectorMessageNotification.class);

    public static final int MESSAGE_RECEIVED = MESSAGE_EVENT_ACTION_START_RANGE + 1;
    public static final int MESSAGE_RESPONSE = MESSAGE_EVENT_ACTION_START_RANGE + 5;
    public static final int MESSAGE_ERROR_RESPONSE = MESSAGE_EVENT_ACTION_START_RANGE + 6;

    public static final int MESSAGE_REQUEST_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 4;
    public static final int MESSAGE_REQUEST_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 3;

    static
    {
        registerAction("receive", MESSAGE_RECEIVED);
        registerAction("response", MESSAGE_RESPONSE);
        registerAction("error response", MESSAGE_ERROR_RESPONSE);

        registerAction("begin request", MESSAGE_REQUEST_BEGIN);
        registerAction("end request", MESSAGE_REQUEST_END);
    }

    private String endpoint;
    private FlowConstruct flowConstruct;

    public BaseConnectorMessageNotification(MuleMessage resource,
                                            String endpoint,
                                            FlowConstruct flowConstruct,
                                            int action)
    {
        super(cloneMessage(resource), action, flowConstruct != null ? flowConstruct.getName() : null);
        this.endpoint = endpoint;
        this.flowConstruct = flowConstruct;
    }


    @Override
    protected String getPayloadToString()
    {
        try
        {
            return ((MuleMessage) source).getPayloadAsString();
        }
        catch (Exception e)
        {
            return source.toString();
        }
    }

    @Override
    public String toString()
    {
        return EVENT_NAME
               + "{action=" + getActionName(action)
               + ", endpoint: " + endpoint
               + ", resourceId=" + resourceIdentifier
               + ", timestamp=" + timestamp
               + ", serverId=" + serverId
               + ", message: " + source + "}";
    }

    public String getEndpoint()
    {
        return endpoint;
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
