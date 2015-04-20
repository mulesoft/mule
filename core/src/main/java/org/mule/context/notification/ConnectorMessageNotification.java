/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;

/**
 * Used to notify that a message was received or sent through a {@link org.mule.api.transport.Connector}.
 */
public class ConnectorMessageNotification extends BaseConnectorMessageNotification
{

    public ConnectorMessageNotification(MuleMessage resource, String connectionPoint, FlowConstruct flowConstruct, int action)
    {
        super(resource, connectionPoint, flowConstruct, action);
    }
}
