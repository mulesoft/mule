/*
 * $$Id$$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.cxf;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.cxf.ClientWrapper;
import org.mule.transport.cxf.CxfMessageDispatcher;

/**
 * Extends {@link CxfMessageDispatcher} just to be able to obtain the
 * {#link ClientWrapper} instance. 
 */
public class CxfMessageDispatcherWrapper extends CxfMessageDispatcher
{

    public CxfMessageDispatcherWrapper(OutboundEndpoint endpoint)
            throws MalformedEndpointException
    {
        super(endpoint);
    }

    public ClientWrapper getClientWrapper() {
        return wrapper;
    }
}
