/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;

/**
 * These notifications are fired when either a message is either: received by an
 * endpoint, sent or dispatched from an endpoint or requested from an endpoint.
 */
public class EndpointMessageNotification extends BaseConnectorMessageNotification
{

    public static final int MESSAGE_DISPATCH_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 2;
    public static final int MESSAGE_SEND_BEGIN = MESSAGE_EVENT_ACTION_START_RANGE + 3;
    public static final int MESSAGE_DISPATCH_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 1;
    public static final int MESSAGE_SEND_END = MESSAGE_EVENT_END_ACTION_START_RANGE + 2;

    static
    {
        registerAction("begin dispatch", MESSAGE_DISPATCH_BEGIN);
        registerAction("begin send", MESSAGE_SEND_BEGIN);

        registerAction("end dispatch", MESSAGE_DISPATCH_END);
        registerAction("end send", MESSAGE_SEND_END);
    }

    /**
     * For backwards compatibility.  BEGIN is chosen where it contains the message sent, and END where it contains the message
     * received, again for backwards compatibility.
     */
    public static final int MESSAGE_DISPATCHED = MESSAGE_DISPATCH_BEGIN;
    public static final int MESSAGE_SENT = MESSAGE_SEND_BEGIN;
    public static final int MESSAGE_REQUESTED = MESSAGE_REQUEST_END;

    private ImmutableEndpoint immutableEndpoint;

    public EndpointMessageNotification(MuleMessage resource,
                               ImmutableEndpoint endpoint,
                               FlowConstruct flowConstruct,
                               int action)
    {
        super(resource, endpoint.getEndpointURI().toString(), flowConstruct, action);
        this.immutableEndpoint = endpoint;
    }

    public ImmutableEndpoint getImmutableEndpoint()
    {
        return immutableEndpoint;
    }

}
