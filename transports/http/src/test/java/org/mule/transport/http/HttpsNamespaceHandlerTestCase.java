/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
