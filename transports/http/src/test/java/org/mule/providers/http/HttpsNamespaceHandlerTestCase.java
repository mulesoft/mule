/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

public class HttpsNamespaceHandlerTestCase extends AbstractNamespaceHandlerTestCase
{

    public HttpsNamespaceHandlerTestCase()
    {
        super("https");
    }

    public void testConnectorProperties()
    {
        HttpsConnector connector =
                (HttpsConnector) managementContext.getRegistry().lookupConnector("httpsConnector");
        testBasicProperties(connector);

        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getKeyStore().endsWith("/serverKeystore"));
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

        assertEquals("foo", connector.getProtocolHandler());
    }

}