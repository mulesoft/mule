/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component.simple;

import org.mule.api.MuleEventContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.Callable;
import org.mule.api.routing.InboundRouter;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.routing.inbound.InboundPassThroughRouter;

import java.util.Iterator;

/**
 * The BridgeComponent is a standard Mule service that enables a bridge between an inbound and outbound endpoints.
 * Transformers can be used on the endpoints to convert the data being received in order to 'bridge' from one
 * endpoint transport to another.  When the BridgeComponent is used, it configures itself so that it will
 * not actually be invoked, instead it tells Mule to bypass invocation of the service, which has a slight performance
 * improvement. Note that because the service is never actually invoked any interceptors configured on the service
 * will not be invoked either.
 *
 * @deprecated along with bridge-service - use an empty service and, if you want an efficient transfer of messages,
 * add a forwarding-consumer.
 */
public class BridgeComponent implements ServiceAware, Callable
{

    public Object onCall(MuleEventContext context) throws Exception
    {
        throw new UnsupportedOperationException(
            "A bridge should not ever receive an event, instead the event should be directly dispatched from the inbound endpoint to the outbound router. Service is: "
                            + context.getService().getName());
    }

    public void setService(Service service) throws ConfigurationException
    {
        // Add a ForwardingConsumer, which punts message to oubound router, unless already present
        boolean registered = false;
        if(service.getInboundRouter()==null)
        {
            service.setInboundRouter(new DefaultInboundRouterCollection());
        }
        for (Iterator routers = service.getInboundRouter().getRouters().iterator(); routers.hasNext();)
        {
            InboundRouter router = (InboundRouter) routers.next();
            //Remove if present
            if(router instanceof InboundPassThroughRouter)
            {
                service.getInboundRouter().removeRouter(router);
            }
            registered = registered || router instanceof ForwardingConsumer;

        }
        if (! registered)
        {
            service.getInboundRouter().addRouter(new ForwardingConsumer());
        }
        // Make sure if other routers on the inbound router, they are honoured
        service.getInboundRouter().setMatchAll(true);
    }

}
