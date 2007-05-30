/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.ssl;

import org.mule.tck.FunctionalTestCase;

public class SslNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "ssl-namespace-config.xml";
    }

    public void testConnectorProperties() throws Exception
    {
        SslConnector connector =
                (SslConnector) managementContext.getRegistry().lookupConnector("sslConnector");
        assertNotNull(connector);
        assertEquals(1024, connector.getSendBufferSize());
        assertEquals(2048, connector.getReceiveBufferSize());
        assertTrue(connector.isKeepAlive());

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
