/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointException;
import org.mule.transaction.MuleTransactionConfig;

import java.util.HashMap;

public class NullInboundEndpoint extends DefaultInboundEndpoint
{
    public NullInboundEndpoint(MessageExchangePattern mep, MuleContext muleContext) throws EndpointException
    {
        super(null, new MuleEndpointURI("dynamic://null", muleContext), null, new HashMap(),
            new MuleTransactionConfig(), false, mep, 0, null, null, null, muleContext, null, null, null,
            null, false, null);
    }

}
