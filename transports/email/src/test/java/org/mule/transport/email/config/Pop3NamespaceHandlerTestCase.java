/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.config;

import org.mule.api.MuleException;
import org.mule.transport.email.Pop3Connector;
import org.mule.transport.email.Pop3sConnector;

import javax.mail.Flags;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Pop3NamespaceHandlerTestCase extends AbstractEmailNamespaceHandlerTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "pop3-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        Pop3Connector c = (Pop3Connector)muleContext.getRegistry().lookupConnector("pop3Connector");
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
        Pop3sConnector c = (Pop3sConnector)muleContext.getRegistry().lookupConnector("pop3sConnector");
        assertNotNull(c);

        assertTrue(c.isBackupEnabled());
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
        
        assertEquals(Flags.Flag.ANSWERED, c.getDefaultProcessMessageAction());
    }

    @Test
    public void testEndpoint() throws MuleException
    {
        testInboundEndpoint("global1", Pop3Connector.POP3);
        testInboundEndpoint("global2", Pop3Connector.POP3);
        testInboundEndpoint("global1s", Pop3sConnector.POP3S);
        testInboundEndpoint("global2s", Pop3sConnector.POP3S);
    }

}
