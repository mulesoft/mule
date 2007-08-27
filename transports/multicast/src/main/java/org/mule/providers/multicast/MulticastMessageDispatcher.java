/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.providers.udp.UdpMessageDispatcher;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>MulticastMessageDispatcher</code> dispatches events to a multicast address
 */

public class MulticastMessageDispatcher extends UdpMessageDispatcher
{

    public MulticastMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

}
