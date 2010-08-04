/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ibeans;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.ibean.IBeansConnector;

public class IBeansConnectorFactoryTestCase extends AbstractMuleTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/display/MULE2USER/Creating+Transports */

    public void testCreateFromFactory() throws Exception
    {
        InboundEndpoint endpoint = muleContext.getRegistry()
                .lookupEndpointFactory().getInboundEndpoint(getEndpointURI());
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof IBeansConnector);
        assertEquals(getEndpointURI(), endpoint.getEndpointURI().toString());
    }

    public String getEndpointURI() 
    {
        return "ibean://hostip.getHostInfo";
    }
}
