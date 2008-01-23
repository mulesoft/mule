/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;

/**
 * Creates an XFire Messgae dispatcher used for making XFire soap requests using the
 * XFire client.
 */
public class XFireMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{
    public MessageDispatcher create(ImmutableEndpoint endpoint) throws MuleException
    {
        return new XFireMessageDispatcher(endpoint);
    }
}
