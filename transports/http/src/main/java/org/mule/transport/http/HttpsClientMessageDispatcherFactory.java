/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * Creates a HttpsClientMessageDispatcher to make client requests
 */
public class HttpsClientMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{

    @Override
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new HttpsClientMessageDispatcher(endpoint);
    }

}


