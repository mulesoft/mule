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

import org.mule.providers.email.ImapsConnector;
import org.mule.providers.email.functional.AbstractEmailFunctionalTestCase;
import org.mule.umo.UMODescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOInboundRouterCollection;

public class ImapsNamespaceHandlerTestCase extends AbstractEmailFunctionalTestCase
{

    public ImapsNamespaceHandlerTestCase()
    {
        super(65433, STRING_MESSAGE, "imaps");
    }

    protected String getConfigResources()
    {
        return "imaps-namespace-config.xml";
    }

    public void testSecureConfig() throws Exception
    {
        ImapsConnector c = (ImapsConnector)managementContext.getRegistry().lookupConnector("imapsConnector");
        assertNotNull(c);

        assertFalse(c.isBackupEnabled());
        assertEquals("newBackup", c.getBackupFolder());
        assertEquals(1234, c.getCheckFrequency());
        assertEquals("newMailbox", c.getMailboxFolder());
        assertEquals(false, c.isDeleteReadMessages());

        // authenticator?

        //The full path gets resolved, we're just checking that the property got set
        assertTrue(c.getClientKeyStore().endsWith("/greenmail-truststore"));
        assertEquals("password", c.getClientKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(c.getTrustStore().endsWith("/greenmail-truststore"));
        assertEquals("password", c.getTrustStorePassword());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    public void testInboundEndpoint()
    {
        UMODescriptor service = managementContext.getRegistry().lookupService("service");
        assertNotNull(service);
        UMOInboundRouterCollection inbound = service.getInboundRouter();
        assertNotNull(inbound);
        testEndpoint(inbound.getEndpoint("in"));
    }

    protected void testEndpoint(UMOEndpoint endpoint)
    {
        assertNotNull(endpoint);
        String address = endpoint.getEndpointURI().getAddress();
        assertNotNull(address);
        assertEquals("bob@localhost:65433", address);
        String password = endpoint.getEndpointURI().getPassword();
        assertNotNull(password);
        assertEquals("password", password);
        String protocol = endpoint.getProtocol();
        assertNotNull(protocol);
        assertEquals("imaps", protocol);
    }
                                     
}