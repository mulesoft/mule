/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
