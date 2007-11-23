/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.providers.AbstractMessageRequesterFactory;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageRequester;

/**
 * <code>VMMessageDispatcherFactory</code> creates an in-memory event dispatcher
 */
public class VMMessageRequesterFactory extends AbstractMessageRequesterFactory
{
    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOMessageDispatcherFactory#create(org.mule.umo.provider.UMOConnector)
     */
    public UMOMessageRequester create(UMOImmutableEndpoint endpoint) throws UMOException
    {
        return new VMMessageRequester(endpoint);
    }
}