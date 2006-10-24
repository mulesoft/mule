/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ConnectorFactoryTestCase extends AbstractMuleTestCase
{
    public void testCreate() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI("ssl://localhost:7877");
        UMOEndpoint endpoint = ConnectorFactory.createEndpoint(url, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertEquals("ssl://localhost:7877", endpoint.getEndpointURI().getAddress());
    }
}
