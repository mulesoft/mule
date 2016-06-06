/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.endpoint;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleException;

/**
 * Cache endpoints in order to prevent memory leaks.
 *
 * see MULE-5422
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface EndpointCache
{
    public InboundEndpoint getInboundEndpoint(String uri, MessageExchangePattern mep) throws MuleException;

    public OutboundEndpoint getOutboundEndpoint(String uri, MessageExchangePattern mep, Long responseTimeout) throws MuleException;
}
