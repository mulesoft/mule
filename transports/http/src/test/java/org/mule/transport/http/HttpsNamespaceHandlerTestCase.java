/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpsNamespaceHandlerTestCase extends AbstractNamespaceHandlerTestCase
{

    public HttpsNamespaceHandlerTestCase()
    {
        super("https");
    }

    @Test
    public void testConnectorProperties()
    {
        HttpsConnector connector =
                (HttpsConnector) muleContext.getRegistry().lookupConnector("httpsConnector");
        testBasicProperties(connector);

        //The full path gets resolved, we're just checking that the property got set
        assertTrue(connector.getKeyStore().endsWith("/serverKeystore"));
        assertEquals("muleserver", connector.getKeyAlias());
        assertEquals("mulepassword", connector.getKeyPassword());
        assertEquals("mulepassword", connector.getKeyStorePassword());
        //The full path gets resolved, we're just checking that the property got set
        assertTrue(connector.getClientKeyStore().endsWith("/clientKeystore"));
        assertEquals("mulepassword", connector.getClientKeyStorePassword());
        //The full path gets resolved, we're just checking that the property got set
        assertTrue(connector.getTrustStore().endsWith("/trustStore"));
        assertEquals("mulepassword", connector.getTrustStorePassword());
        assertTrue(connector.isExplicitTrustStoreOnly());
        assertTrue(connector.isRequireClientAuthentication());

        assertEquals("foo", connector.getProtocolHandler());
    }

    @Test
    public void testPollingProperties()
    {
         HttpsPollingConnector connector =
                (HttpsPollingConnector) muleContext.getRegistry().lookupConnector("polling");
        assertNotNull(connector);
        assertEquals(3456, connector.getPollingFrequency());
        assertFalse(connector.isCheckEtag());
        assertFalse(connector.isDiscardEmptyContent());
    }
}
