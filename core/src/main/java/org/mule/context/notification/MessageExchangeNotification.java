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
 * These notifications are fired when either a message is received from or dispatched to an external entity
 */
public class MessageExchangeNotification extends ServerNotification
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -5118299601117624094L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MessageExchangeNotification.class);

    public static final int MESSAGE_RECEIVED = MESSAGE_EVENT_ACTION_START_RANGE + 1;
    public static final int MESSAGE_DISPATCH_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 2;
    public static final int MESSAGE_SEND_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 3;
    public static final int MESSAGE_REQUEST_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 4;
    public static final int MESSAGE_RESPONSE = MESSAGE_EVENT_ACTION_START_RANGE + 5;

    public static final int MESSAGE_DISPATCH_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 1;
    public static final int MESSAGE_SEND_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 2;
    public static final int MESSAGE_REQUEST_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 3;

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

    private String exchangePoint;
    private FlowConstruct flowConstruct;

    public MessageExchangeNotification(MuleMessage resource,
                                       String exchangePoint,
                                       FlowConstruct flowConstruct,
                                       int action)
    {
        super(cloneMessage(resource), action, flowConstruct != null ? flowConstruct.getName() : null);
        this.exchangePoint = exchangePoint;
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
               + ", exchangePoint: " + exchangePoint
               + ", resourceId=" + resourceIdentifier
               + ", timestamp=" + timestamp
               + ", serverId=" + serverId
               + ", message: " + source + "}";
    }

    public String getExchangePoint()
    {
        return exchangePoint;
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
