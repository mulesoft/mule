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

import org.mule.providers.email.ImapConnector;
import org.mule.providers.email.functional.AbstractEmailFunctionalTestCase;
import org.mule.umo.UMODescriptor;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * This includes a connection because the IMAP poller starts by default (without
 * the server running we cannot easily start an inbound endpoint)
 */
public class ImapNamespaceHandlerTestCase extends AbstractEmailFunctionalTestCase
{

    public ImapNamespaceHandlerTestCase()
    {
        super(65532, STRING_MESSAGE, "imap", "imap-namespace-config.xml");
    }

    public void testConfig() throws Exception
    {
        ImapConnector c = (ImapConnector)managementContext.getRegistry().lookupConnector("imapConnector");
        assertNotNull(c);

        assertTrue(c.isBackupEnabled());
        assertEquals("newBackup", c.getBackupFolder());
        assertEquals(1234, c.getCheckFrequency());
        assertEquals("newMailbox", c.getMailboxFolder());
        assertEquals(false, c.isDeleteReadMessages());

        // authenticator?

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    public void testGlobalEndpoint()
    {
        testEndpoint(managementContext.getRegistry().lookupEndpoint("global"));
    }

    protected void testEndpoint(UMOEndpoint endpoint)
    {
        assertNotNull(endpoint);
        String address = endpoint.getEndpointURI().getAddress();
        assertNotNull(address);
        assertEquals("bob@localhost:65532", address);
        String password = endpoint.getEndpointURI().getPassword();
        assertNotNull(password);
        assertEquals("password", password);
        String protocol = endpoint.getProtocol();
        assertNotNull(protocol);
        assertEquals("imap", protocol);
    }

    public void testInboundEndpoint()
    {
        UMODescriptor service = managementContext.getRegistry().lookupService("service");
        assertNotNull(service);
        UMOInboundRouterCollection inbound = service.getInboundRouter();
        assertNotNull(inbound);
        testEndpoint(inbound.getEndpoint("in"));
    }

}