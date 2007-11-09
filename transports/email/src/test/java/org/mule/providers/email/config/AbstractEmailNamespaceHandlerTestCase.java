/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email.config;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public abstract class AbstractEmailNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected void testEndpoint(String name, String protocolName) throws UMOException
    {
        UMOImmutableEndpoint endpoint =
                managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(name, managementContext);
        assertNotNull(endpoint);
        String address = endpoint.getEndpointURI().getAddress();
        assertNotNull(address);
        assertEquals("bob@localhost:123", address);
        String password = endpoint.getEndpointURI().getPassword();
        assertNotNull(password);
        assertEquals("secret", password);
        String protocol = endpoint.getProtocol();
        assertNotNull(protocol);
        assertEquals(protocolName, protocol);
    }

}