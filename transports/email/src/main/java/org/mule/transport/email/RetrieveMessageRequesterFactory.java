/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageRequester;
import org.mule.transport.AbstractMessageRequesterFactory;

/**
 * A source of mail receiving message dispatchers.
 * The dispatcher can only be used to receive message (as apposed to
 * listening for them). Trying to send or dispatch will throw an
 * {@link UnsupportedOperationException}.
 */

public class RetrieveMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    private boolean requesterPerRequest = true;
    /**
     * By default client connections are closed after the request.
     */
    @Override
    public boolean isCreateRequesterPerRequest()
    {
        return requesterPerRequest;
    }

    @Override
    public MessageRequester create(InboundEndpoint endpoint) throws MuleException
    {
        return new RetrieveMessageRequester(endpoint);
    }
}
