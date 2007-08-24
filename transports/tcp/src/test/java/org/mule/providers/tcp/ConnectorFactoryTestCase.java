/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.TransportFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpoint;

public class ConnectorFactoryTestCase extends AbstractMuleTestCase
{
    public void testCreate() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI("tcp://7877");
        UMOEndpoint endpoint = TransportFactory.createEndpoint(url, UMOEndpoint.ENDPOINT_TYPE_RECEIVER, managementContext);
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertEquals("tcp://localhost:7877", endpoint.getEndpointURI().getAddress());
    }
}
