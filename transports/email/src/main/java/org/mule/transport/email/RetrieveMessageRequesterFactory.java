/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
