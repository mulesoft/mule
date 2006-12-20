/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsMessageDispatcher;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class OracleJmsMessageDispatcher extends JmsMessageDispatcher
{

    public OracleJmsMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    /**
     * Make a specific request to the underlying transport Save a copy of the
     * endpoint's properties within the OracleJmsSupport object.
     * 
     * @see OracleJmsSupport#endpointProperties
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        ((OracleJmsSupport)((JmsConnector)getConnector()).getJmsSupport()).setEndpointProperties(endpoint.getEndpointURI()
            .getParams());
        return super.doReceive(timeout);
    }
}
