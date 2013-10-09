/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
