/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ejb;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.rmi.RmiMessageReceiver;

/**
 * Will repeatedly call a method on an EJB object. If the method takes parameters A
 * List of objects can be specified on the endpoint called
 * <code>methodArgumentsList</code>, If this property is ommitted it is assumed
 * that the method takes no parameters
 */

public class EjbMessageReceiver extends RmiMessageReceiver
{

    public EjbMessageReceiver(Connector connector,
                              Service service,
                              ImmutableEndpoint endpoint,
                              long frequency) throws CreateException
    {
        super(connector, service, endpoint, frequency);

        this.connector = (EjbConnector) connector;
    }

}
