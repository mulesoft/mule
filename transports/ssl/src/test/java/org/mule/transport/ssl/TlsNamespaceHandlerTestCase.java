/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class TlsNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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

    @Test
    public void testTlsContextProperties() throws Exception
    {
        TlsContext tlsContext = muleContext.getRegistry().get("tlsContext");

        assertThat(tlsContext.getTrustStorePath(), endsWith("testTrustStorePath"));
        assertThat(tlsContext.getTrustStorePassword(), equalTo("testTrustStorePassword"));
        assertThat(tlsContext.getTrustStoreType(), equalTo("testTrustStoreType"));
        assertThat(tlsContext.getTrustManagerAlgorithm(), equalTo("testTrustStoreAlgorithm"));
        assertThat(tlsContext.getKeyStorePath(), endsWith("testKeyStorePath"));
        assertThat(tlsContext.getKeyStorePassword(), equalTo("testKeyStorePassword"));
        assertThat(tlsContext.getKeyStoreType(), equalTo("testKeyStoreType"));
        assertThat(tlsContext.getKeyManagerPassword(), equalTo("testKeyPassword"));
        assertThat(tlsContext.getKeyManagerAlgorithm(), equalTo("testKeyStoreAlgorithm"));
    }
}
