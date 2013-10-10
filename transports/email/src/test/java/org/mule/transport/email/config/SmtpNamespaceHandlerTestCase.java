/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.config;

import org.mule.api.MuleException;
import org.mule.transport.email.SmtpConnector;
import org.mule.transport.email.SmtpsConnector;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SmtpNamespaceHandlerTestCase extends AbstractEmailNamespaceHandlerTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "smtp-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        SmtpConnector c = (SmtpConnector)muleContext.getRegistry().lookupConnector("smtpConnector");
        assertNotNull(c);

        assertEquals("abc@example.com", c.getBccAddresses());
        assertEquals("bcd@example.com", c.getCcAddresses());
        assertEquals("foo/bar", c.getContentType());
        Properties headers = c.getCustomHeaders();
        assertEquals(2, headers.size());
        assertEquals("bar", headers.getProperty("foo"));
        assertEquals("boz", headers.getProperty("baz"));
        assertEquals("cde@example.com", c.getFromAddress());
        assertEquals("def@example.com", c.getReplyToAddresses());
        assertEquals("subject", c.getSubject());

        // authenticator?

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testSecureConfig() throws Exception
    {
        SmtpsConnector c = (SmtpsConnector)muleContext.getRegistry().lookupConnector("smtpsConnector");
        assertNotNull(c);

        assertEquals("abc@example.com", c.getBccAddresses());
        assertEquals("bcd@example.com", c.getCcAddresses());
        assertEquals("foo/bar", c.getContentType());
        Properties headers = c.getCustomHeaders();
        assertEquals(2, headers.size());
        assertEquals("bar", headers.getProperty("foo"));
        assertEquals("boz", headers.getProperty("baz"));
        assertEquals("cde@example.com", c.getFromAddress());
        assertEquals("def@example.com", c.getReplyToAddresses());
        assertEquals("subject", c.getSubject());

        // authenticator?

        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(c.getClientKeyStore().endsWith("/empty.jks"));
        assertEquals("password", c.getClientKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(c.getTrustStore().endsWith("/empty.jks"));
        assertEquals("password", c.getTrustStorePassword());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testEndpoint() throws MuleException
    {
        testOutboundEndpoint("global1", SmtpConnector.SMTP);
        testOutboundEndpoint("global2", SmtpConnector.SMTP);
        testOutboundEndpoint("global1s", SmtpsConnector.SMTPS);
        testOutboundEndpoint("global2s", SmtpsConnector.SMTPS);
    }
}
