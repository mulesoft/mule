/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.tls;

import org.junit.Test;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.api.tls.TlsContextFactory;
import org.mule.api.tls.TlsContextKeyStoreConfiguration;
import org.mule.api.tls.TlsContextTrustStoreConfiguration;
import org.mule.module.tls.internal.DefaultTlsContextFactory;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TlsNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "tls-namespace-config.xml";
    }

    @Test
    public void testTlsContextProperties() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = muleContext.getRegistry().get("tlsContext");

        assertThat(tlsContextFactory.getTrustStorePath(), endsWith("trustStore"));
        assertThat(tlsContextFactory.getTrustStorePassword(), equalTo("testTrustStorePassword"));
        assertThat(tlsContextFactory.getTrustStoreType(), equalTo("testTrustStoreType"));
        assertThat(tlsContextFactory.getTrustManagerAlgorithm(), equalTo("testTrustStoreAlgorithm"));
        assertThat(tlsContextFactory.getKeyStorePath(), endsWith("serverKeystore"));
        assertThat(tlsContextFactory.getKeyStorePassword(), equalTo("testKeyStorePassword"));
        assertThat(tlsContextFactory.getKeyStoreType(), equalTo("testKeyStoreType"));
        assertThat(tlsContextFactory.getKeyManagerPassword(), equalTo("testKeyPassword"));
        assertThat(tlsContextFactory.getKeyManagerAlgorithm(), equalTo("testKeyStoreAlgorithm"));
        assertThat(tlsContextFactory.getKeyAlias(), equalTo("testKeyStoreAlias"));
    }

    @Test
    public void testTlsContextKeyStoreProperties() throws Exception
    {
        TlsContextFactory tlsContextFactory = muleContext.getRegistry().get("tlsContext");
        TlsContextKeyStoreConfiguration keyStoreConfig = tlsContextFactory.getKeyStoreConfiguration();

        assertThat(keyStoreConfig.getPath(), endsWith("serverKeystore"));
        assertThat(keyStoreConfig.getPassword(), equalTo("testKeyStorePassword"));
        assertThat(keyStoreConfig.getType(), equalTo("testKeyStoreType"));
        assertThat(keyStoreConfig.getKeyPassword(), equalTo("testKeyPassword"));
        assertThat(keyStoreConfig.getAlgorithm(), equalTo("testKeyStoreAlgorithm"));
        assertThat(keyStoreConfig.getAlias(), equalTo("testKeyStoreAlias"));
    }

    @Test
    public void testTlsContextTrustStoreProperties() throws Exception
    {
        TlsContextFactory tlsContextFactory = muleContext.getRegistry().get("tlsContext");
        TlsContextTrustStoreConfiguration trustStoreConfig = tlsContextFactory.getTrustStoreConfiguration();

        assertThat(trustStoreConfig.getPath(), endsWith("trustStore"));
        assertThat(trustStoreConfig.getPassword(), equalTo("testTrustStorePassword"));
        assertThat(trustStoreConfig.getType(), equalTo("testTrustStoreType"));
        assertThat(trustStoreConfig.getAlgorithm(), equalTo("testTrustStoreAlgorithm"));
    }



}
