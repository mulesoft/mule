/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * Creates a CXF Message dispatcher used for making requests using the CXF client.
 */
public class CxfMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new CxfMessageDispatcher(endpoint);
    }
}
