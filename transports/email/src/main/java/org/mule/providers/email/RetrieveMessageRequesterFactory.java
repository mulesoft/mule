/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.providers.AbstractMessageRequesterFactory;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageRequester;

/**
 * A source of mail receiving message dispatchers.
 * The dispatcher can only be used to receive message (as apposed to
 * listening for them). Trying to send or dispatch will throw an
 * {@link UnsupportedOperationException}.
 */

public class RetrieveMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    /**
     * By default client connections are closed after the request.
     */
    // @Override
    public boolean isCreateRequesterPerRequest()
    {
        return true;
    }

    public UMOMessageRequester create(UMOImmutableEndpoint endpoint) throws UMOException
    {
        return new RetrieveMessageRequester(endpoint);
    }

}