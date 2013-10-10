/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageRequester;
import org.mule.transport.AbstractMessageRequesterFactory;

/**
 * Creates a HttpClientMessageDispatcher to make client requests
 */
public class HttpClientMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    /** {@inheritDoc} */
    public MessageRequester create(InboundEndpoint endpoint) throws MuleException
    {
        return new HttpClientMessageRequester(endpoint);
    }
}
