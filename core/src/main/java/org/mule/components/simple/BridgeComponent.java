/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.simple;

import org.mule.impl.UMODescriptorAware;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.routing.UMOInboundRouter;

import java.util.Iterator;

/**
 * Can be used to bridge inbound requests to an outbound router without any
 * processing done inbetween.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class BridgeComponent implements UMODescriptorAware, Callable
{

    public void setDescriptor(UMODescriptor descriptor)
    {
        // Adding a forwarding consumer will cause the inbound routing to
        // directly invoke the outbound router, bypassing the component

        // first check there isn't one already registered
        boolean registered = false;
        for (Iterator iterator = descriptor.getInboundRouter().getRouters().iterator(); iterator.hasNext();)
        {
            UMOInboundRouter router = (UMOInboundRouter)iterator.next();
            if (router instanceof ForwardingConsumer)
            {
                registered = true;
            }

        }
        if (!registered)
        {
            descriptor.getInboundRouter().addRouter(new ForwardingConsumer());
        }
        // Make sure if other routers on the inbound router, they are honoured
        descriptor.getInboundRouter().setMatchAll(true);
    }

    public Object onCall(UMOEventContext context) throws Exception
    {
        throw new UnsupportedOperationException(
            "A bridge should not ever receive an event, instead the event should be directly dispatched from the inbound endpoint to the outbound router. Component is: "
                            + context.getComponentDescriptor().getName());
    }
}
