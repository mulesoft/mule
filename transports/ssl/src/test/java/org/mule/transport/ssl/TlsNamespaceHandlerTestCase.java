/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TlsNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "tls-namespace-config.xml";
    }

    @Test
    public void testConnectorProperties() throws Exception
    {
        SslConnector connector = (SslConnector)muleContext.getRegistry().lookupConnector("sslConnector");
        assertNotNull(connector);
        assertEquals(1024, connector.getSendBufferSize());
        assertEquals(2048, connector.getReceiveBufferSize());
        assertTrue(connector.isKeepAlive());

        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getKeyStore().endsWith("/serverKeystore"));
        assertEquals("muleserver", connector.getKeyAlias());
        assertEquals("mulepassword", connector.getKeyPassword());
        assertEquals("mulepassword", connector.getKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getClientKeyStore().endsWith("/clientKeystore"));
        assertEquals("mulepassword", connector.getClientKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getTrustStore().endsWith("/trustStore"));
        assertEquals("mulepassword", connector.getTrustStorePassword());
        assertTrue(connector.isExplicitTrustStoreOnly());
        assertTrue(connector.isRequireClientAuthentication());
    }
}
