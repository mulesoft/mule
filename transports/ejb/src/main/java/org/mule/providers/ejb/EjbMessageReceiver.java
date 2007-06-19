/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.providers.rmi.RmiMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;

/**
 * Will repeatedly call a method on an EJB object. If the method takes parameters A
 * List of objects can be specified on the endpoint called
 * <code>methodArgumentsList</code>, If this property is ommitted it is assumed
 * that the method takes no parameters
 */

public class EjbMessageReceiver extends RmiMessageReceiver
{

    public EjbMessageReceiver(UMOConnector connector,
                              UMOComponent component,
                              UMOEndpoint endpoint,
                              long frequency) throws CreateException
    {
        super(connector, component, endpoint, frequency);

        this.connector = (EjbConnector) connector;
    }

}
