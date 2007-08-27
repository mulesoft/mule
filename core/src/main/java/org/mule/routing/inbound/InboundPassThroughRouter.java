/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.umo.UMOEvent;
import org.mule.umo.routing.RoutingException;

/**
 * <code>InboundPassThroughRouter</code> allows inbound routing over all
 * registered endpoints without any filtering. This class is used by Mule when a
 * specific inbound router has not been configured on a UMODescriptor.
 */

public class InboundPassThroughRouter extends SelectiveConsumer
{
    public UMOEvent[] process(UMOEvent event) throws RoutingException
    {
        synchronized (event)
        {
            return new UMOEvent[]{event};
        }
    }

    public boolean isMatch(UMOEvent event) throws RoutingException
    {
        return true;
    }
}
