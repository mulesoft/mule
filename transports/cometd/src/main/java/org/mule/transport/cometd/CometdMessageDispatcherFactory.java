/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd;

import org.mule.transport.AbstractMessageDispatcherFactory;
import org.mule.transport.cometd.embedded.CometdConnector;
import org.mule.transport.cometd.container.CometdServletConnector;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.MuleException;

import org.mortbay.cometd.AbstractBayeux;

/**
 * Creates a {@link CometdMessageDispatcher}
 */
public class CometdMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        CometdMessageDispatcher dispatcher = new CometdMessageDispatcher(endpoint);

        if(endpoint.getConnector() instanceof CometdConnector)
        {
            //We're running in embedded mode (i.e. using a Jetty servlet container created by the connector)
            //so we need to register the endpoint
            CometdConnector.BayeuxHolder holder = ((CometdConnector)endpoint.getConnector()).registerBayeuxEndpoint(endpoint);
            dispatcher.setBayeux(holder.getBayeux());
        }
        else
        {
            //We're bound to an existing servlet container, just grab the Bayeux object from the connector, which  would have been
            //set by the {@link MuleCometdServlet}
            AbstractBayeux b = ((CometdServletConnector)endpoint.getConnector()).getBayeux();
            if(b==null)
            {
                throw new IllegalArgumentException("Bayeux is null: " + endpoint.getConnector());
            }
            dispatcher.setBayeux(b);
        }


        return dispatcher;
    }
}
