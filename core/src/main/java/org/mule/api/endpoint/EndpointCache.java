/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;

/**
 * Cache endpoints in order to prevent memory leaks.
 *
 * see MULE-5422
 */
public interface EndpointCache
{
    public InboundEndpoint getInboundEndpoint(String uri, MessageExchangePattern mep) throws MuleException;

    public OutboundEndpoint getOutboundEndpoint(String uri, MessageExchangePattern mep, Long responseTimeout) throws MuleException;
}
