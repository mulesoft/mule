/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.stdio;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * <code>StdioMessageDispatcherFactory</code> creates a Stream dispatcher suitable
 * for writing to fixed streams such as System.in or System.out.
 */
public class StdioMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.transport.MessageDispatcherFactory#create(org.mule.api.transport.Connector)
     */
    public MessageDispatcher create(ImmutableEndpoint endpoint) throws MuleException
    {
        return new StdioMessageDispatcher(endpoint);
    }
}
