/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email;

import org.mule.tck.FunctionalTestCase;

/**
 * TODO
 */
public class Pop3NamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "pop3-namespace-config.xml";
    }

    public void testConfig() throws Exception
    {
        Pop3Connector c = (Pop3Connector)managementContext.getRegistry().lookupConnector("pop3Connector");
        assertNotNull(c);

        assertEquals("newBackup", c.getBackupFolder());
        assertEquals(1234, c.getCheckFrequency());
        assertEquals("newMailbox", c.getMailboxFolder());
        assertEquals(false, c.isDeleteReadMessages());

        // authenticator?

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

    }

    public void testSecureConfig() throws Exception
    {
        Pop3sConnector c = (Pop3sConnector)managementContext.getRegistry().lookupConnector("pop3sConnector");
        assertNotNull(c);

        assertEquals("newBackup", c.getBackupFolder());
        assertEquals(1234, c.getCheckFrequency());
        assertEquals("newMailbox", c.getMailboxFolder());
        assertEquals(false, c.isDeleteReadMessages());

        // authenticator?

        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(c.getClientKeyStore().endsWith("/greenmail-truststore"));
        assertEquals("password", c.getClientKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(c.getTrustStore().endsWith("/greenmail-truststore"));
        assertEquals("password", c.getTrustStorePassword());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

}