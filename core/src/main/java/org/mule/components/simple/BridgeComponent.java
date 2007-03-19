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

import org.mule.config.ConfigurationException;
import org.mule.impl.UMODescriptorAware;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.routing.UMOInboundRouter;

import java.util.Iterator;

/**
 * The BridgeComponent is a standard Mule component that enables a bridge between an inbound and outbound endpoints.
 * Transformers can be used on the endpoints to convert the data being received in order to 'bridge' from one
 * endpoint transport to another.  When the BridgeComponent is used, it configures itself so that it will
 * not actually be invoked, instead it tells Mule to bypass invocation of the component, which has a slight performance
 * improvement. Note that because the component is never actually invoked any interceptors configured on the component
 * will not be invoked either.
 */
public class BridgeComponent implements UMODescriptorAware, Callable
{

    public void setDescriptor(UMODescriptor descriptor) throws ConfigurationException
    {
        // Adding a forwarding consumer will cause the inbound routing to
        // directly invoke the outbound router, bypassing the component

        // first check there isn't one already registered
        boolean registered = false;
        for (Iterator iterator = descriptor.getInboundRouter().getRouters().iterator(); iterator.hasNext();)
        {
            UMOInboundRouter router = (UMOInboundRouter) iterator.next();
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
