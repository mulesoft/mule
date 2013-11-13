/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.transport.email.ImapConnector;
import org.mule.transport.email.ImapsConnector;

import javax.mail.Flags;

import org.junit.Test;

public class ImapNamespaceHandlerTestCase extends AbstractEmailNamespaceHandlerTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "imap-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        ImapConnector c = (ImapConnector)muleContext.getRegistry().lookupConnector("imapConnector");
        assertNotNull(c);

        assertTrue(c.isBackupEnabled());
        assertEquals("newBackup", c.getBackupFolder());
        assertEquals(1234, c.getCheckFrequency());
        assertEquals("newMailbox", c.getMailboxFolder());
        assertEquals(false, c.isDeleteReadMessages());

        // authenticator?

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
        
        assertEquals(Flags.Flag.SEEN, c.getDefaultProcessMessageAction());
    }

    @Test
    public void testSecureConfig() throws Exception
    {
        ImapsConnector c = (ImapsConnector)muleContext.getRegistry().lookupConnector("imapsConnector");
        assertNotNull(c);

        assertFalse(c.isBackupEnabled());
        assertEquals("newBackup", c.getBackupFolder());
        assertEquals(1234, c.getCheckFrequency());
        assertEquals("newMailbox", c.getMailboxFolder());
        assertEquals(false, c.isDeleteReadMessages());

        // authenticator?

        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(c.getClientKeyStore().endsWith("/empty.jks"));
        assertEquals("password", c.getClientKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(c.getTrustStore().endsWith("/empty.jks"));
        assertEquals("password", c.getTrustStorePassword());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
        
        assertNull(c.getDefaultProcessMessageAction());
    }

    @Test
    public void testEndpoint() throws MuleException
    {
        testInboundEndpoint("global1", ImapConnector.IMAP);
        testInboundEndpoint("global2", ImapConnector.IMAP);
        testInboundEndpoint("global1s", ImapsConnector.IMAPS);
        testInboundEndpoint("global2s", ImapsConnector.IMAPS);
    }

}
