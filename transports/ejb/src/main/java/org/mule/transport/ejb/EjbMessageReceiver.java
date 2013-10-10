/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ejb;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.rmi.RmiMessageReceiver;

/**
 * Will repeatedly call a method on an EJB object. If the method takes parameters A
 * List of objects can be specified on the endpoint called
 * <code>methodArgumentTypes</code>, If this property is ommitted it is assumed
 * that the method takes no parameters
 */

public class EjbMessageReceiver extends RmiMessageReceiver
{

    public EjbMessageReceiver(Connector connector,
                              FlowConstruct flowConstruct,
                              InboundEndpoint endpoint,
                              long frequency) throws CreateException
    {
        super(connector, flowConstruct, endpoint, frequency);

        this.connector = (EjbConnector) connector;
    }

}
