/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi.support;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.component.DefaultInterfaceBinding;

/**
 * Used for making a request from a message dispatcher
 */
public class DefaultRequestInterfaceBinding extends DefaultInterfaceBinding
{

    // The router used to actually request the message
    protected InboundEndpoint inboundEndpoint;

    public void setEndpoint(ImmutableEndpoint e)
    {
        if (e instanceof InboundEndpoint)
        {
            inboundEndpoint = (InboundEndpoint) e;

        }
        else
        {
            throw new IllegalArgumentException("An inbound endpoint is required for Request Interface binding");
        }
    }

    public ImmutableEndpoint getEndpoint()
    {
        return inboundEndpoint;
    }
}
