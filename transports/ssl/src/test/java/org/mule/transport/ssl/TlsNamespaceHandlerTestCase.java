/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextKeyStoreConfiguration;
import org.mule.transport.ssl.api.TlsContextTrustStoreConfiguration;

import org.junit.Test;

public class TlsNamespaceHandlerTestCase extends FunctionalTestCase
{

    private static final String PASSWORD = "mulepassword";
    private static final String ALIAS = "muleserver";
    private static final String TYPE = "jks";
    private static final String ALGORITHM = "SunX509";

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
        assertEquals(ALIAS, connector.getKeyAlias());
        assertEquals(PASSWORD, connector.getKeyPassword());
        assertEquals(PASSWORD, connector.getKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getClientKeyStore().endsWith("/clientKeystore"));
        assertEquals(PASSWORD, connector.getClientKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getTrustStore().endsWith("/trustStore"));
        assertEquals(PASSWORD, connector.getTrustStorePassword());
        assertTrue(connector.isExplicitTrustStoreOnly());
        assertTrue(connector.isRequireClientAuthentication());
    }

    @Test
    public void testTlsContextProperties() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = muleContext.getRegistry().get("tlsContext");

        assertThat(tlsContextFactory.getEnabledProtocols(), is(new String[]{"TLSv1"}));
        assertThat(tlsContextFactory.getEnabledCipherSuites(), is(new String[]{"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"}));
        assertThat(tlsContextFactory.getTrustStorePath(), endsWith("trustStore"));
        assertThat(tlsContextFactory.getTrustStorePassword(), equalTo(PASSWORD));
        assertThat(tlsContextFactory.getTrustStoreType(), equalTo(TYPE));
        assertThat(tlsContextFactory.getTrustManagerAlgorithm(), equalTo(ALGORITHM));
        assertThat(tlsContextFactory.getKeyStorePath(), endsWith("serverKeystore"));
        assertThat(tlsContextFactory.getKeyStorePassword(), equalTo(PASSWORD));
        assertThat(tlsContextFactory.getKeyStoreType(), equalTo(TYPE));
        assertThat(tlsContextFactory.getKeyManagerPassword(), equalTo(PASSWORD));
        assertThat(tlsContextFactory.getKeyManagerAlgorithm(), equalTo(ALGORITHM));
        assertThat(tlsContextFactory.getKeyAlias(), equalTo(ALIAS));
        assertThat(tlsContextFactory.isTrustStoreInsecure(), is(false));
    }

    @Test
    public void testTlsContextKeyStoreProperties() throws Exception
    {
        TlsContextFactory tlsContextFactory = muleContext.getRegistry().get("tlsContext");
        TlsContextKeyStoreConfiguration keyStoreConfig = tlsContextFactory.getKeyStoreConfiguration();

        assertThat(keyStoreConfig.getPath(), endsWith("serverKeystore"));
        assertThat(keyStoreConfig.getPassword(), equalTo(PASSWORD));
        assertThat(keyStoreConfig.getType(), equalTo(TYPE));
        assertThat(keyStoreConfig.getKeyPassword(), equalTo(PASSWORD));
        assertThat(keyStoreConfig.getAlgorithm(), equalTo(ALGORITHM));
        assertThat(keyStoreConfig.getAlias(), equalTo(ALIAS));
    }

    @Test
    public void testTlsContextTrustStoreProperties() throws Exception
    {
        TlsContextFactory tlsContextFactory = muleContext.getRegistry().get("tlsContext");
        TlsContextTrustStoreConfiguration trustStoreConfig = tlsContextFactory.getTrustStoreConfiguration();

        assertThat(trustStoreConfig.getPath(), endsWith("trustStore"));
        assertThat(trustStoreConfig.getPassword(), equalTo(PASSWORD));
        assertThat(trustStoreConfig.getType(), equalTo(TYPE));
        assertThat(trustStoreConfig.getAlgorithm(), equalTo(ALGORITHM));
        assertThat(trustStoreConfig.isInsecure(), equalTo(false));
    }



}
