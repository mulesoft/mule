/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

    private boolean requesterPErRequest = true;
    /**
     * By default client connections are closed after the request.
     */
    @Override
    public boolean isCreateRequesterPerRequest()
    {
        return requesterPErRequest;
    }

    public MessageRequester create(InboundEndpoint endpoint) throws MuleException
    {
        //Do not dispose the Requestor (close the mail folder when using IMAP since the message will stil be on the server)
        if(endpoint.getEndpointURI().getScheme().startsWith("imap"))
        {
            requesterPErRequest = false;
        }
        return new RetrieveMessageRequester(endpoint);
    }

}