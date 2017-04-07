/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.nameable;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractConnector;

/**
 * <code>AbstractInboundEndpointNameableConnector</code> provides the identity criteria for those 
 * connectors that may be distinguished by name as well as by address 
 */
public abstract class AbstractInboundEndpointNameableConnector extends AbstractConnector
{

    public AbstractInboundEndpointNameableConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        {
            return defaultIfEmpty(endpoint.getName(),
                    endpoint.getEndpointURI().getAddress());
        }
    }

}
