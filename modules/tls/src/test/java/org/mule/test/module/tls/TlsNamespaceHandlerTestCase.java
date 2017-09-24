/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.tls;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextKeyStoreConfiguration;
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;

import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;

public class TlsNamespaceHandlerTestCase extends MuleArtifactFunctionalTestCase {

  private static final String PASSWORD = "mulepassword";
  private static final String ALIAS = "muleserver";
  private static final String TYPE = "jks";
  private static final String ALGORITHM = "SunX509";

  @Inject
  @Named("tlsContext")
  private TlsContextFactory tlsContextFactory;

  @Override
  protected String getConfigFile() {
    return "tls-namespace-config.xml";
  }

  @Test
  public void testTlsContextProperties() throws Exception {
    assertThat(tlsContextFactory.getEnabledProtocols(), is(new String[] {"TLSv1"}));
    assertThat(tlsContextFactory.getEnabledCipherSuites(), is(new String[] {"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"}));

    TlsContextTrustStoreConfiguration trustStoreConfiguration = tlsContextFactory.getTrustStoreConfiguration();
    assertThat(trustStoreConfiguration.getPath(), endsWith("trustStore"));
    assertThat(trustStoreConfiguration.getPassword(), equalTo(PASSWORD));
    assertThat(trustStoreConfiguration.getAlgorithm(), equalTo(ALGORITHM));
    assertThat(trustStoreConfiguration.getType(), equalTo(TYPE));

    TlsContextKeyStoreConfiguration keyStoreConfiguration = tlsContextFactory.getKeyStoreConfiguration();
    assertThat(keyStoreConfiguration.getPath(), endsWith("serverKeystore"));
    assertThat(keyStoreConfiguration.getPassword(), equalTo(PASSWORD));
    assertThat(keyStoreConfiguration.getType(), equalTo(TYPE));
    assertThat(keyStoreConfiguration.getKeyPassword(), equalTo(PASSWORD));
    assertThat(keyStoreConfiguration.getAlias(), equalTo(ALIAS));
    assertThat(keyStoreConfiguration.getAlgorithm(), equalTo(ALGORITHM));
  }

  @Test
  public void testTlsContextKeyStoreProperties() throws Exception {
    TlsContextKeyStoreConfiguration keyStoreConfig = tlsContextFactory.getKeyStoreConfiguration();

    assertThat(keyStoreConfig.getPath(), endsWith("serverKeystore"));
    assertThat(keyStoreConfig.getPassword(), equalTo(PASSWORD));
    assertThat(keyStoreConfig.getType(), equalTo(TYPE));
    assertThat(keyStoreConfig.getKeyPassword(), equalTo(PASSWORD));
    assertThat(keyStoreConfig.getAlgorithm(), equalTo(ALGORITHM));
    assertThat(keyStoreConfig.getAlias(), equalTo(ALIAS));
  }

  @Test
  public void testTlsContextTrustStoreProperties() throws Exception {
    TlsContextTrustStoreConfiguration trustStoreConfig = tlsContextFactory.getTrustStoreConfiguration();

    assertThat(trustStoreConfig.getPath(), endsWith("trustStore"));
    assertThat(trustStoreConfig.getPassword(), equalTo(PASSWORD));
    assertThat(trustStoreConfig.getType(), equalTo(TYPE));
    assertThat(trustStoreConfig.getAlgorithm(), equalTo(ALGORITHM));
    assertThat(trustStoreConfig.isInsecure(), equalTo(false));
  }
}
