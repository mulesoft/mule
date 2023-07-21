/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.tls;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;
import org.mule.runtime.api.tls.TlsContextKeyStoreConfiguration;
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactoryBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultTlsContextFactoryBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final TlsContextFactory defaultFactoryMock = mock(TlsContextFactory.class);
  private final TlsContextFactoryBuilder builder = new DefaultTlsContextFactoryBuilder(defaultFactoryMock);

  @Test
  public void buildsContext() throws Exception {
    TlsContextFactory contextFactory = builder
        .enabledCipherSuites("TLS_SOMETHING")
        .enabledProtocols("TLSv1.1")
        .keyStorePath("serverKeystore")
        .keyStorePassword("mulepassword")
        .keyAlias("muleserver")
        .keyPassword("mulepassword")
        .keyStoreAlgorithm("PKIX")
        .trustStorePath("trustStore")
        .trustStorePassword("mulepassword")
        .trustStoreType("jceks")
        .insecureTrustStore(true)
        .build();

    assertThat(contextFactory.getEnabledProtocols(), is(arrayContaining("TLSv1.1")));
    assertThat(contextFactory.getEnabledCipherSuites(), is(arrayContaining("TLS_SOMETHING")));

    TlsContextKeyStoreConfiguration keyStoreConfiguration = contextFactory.getKeyStoreConfiguration();
    assertThat(keyStoreConfiguration.getPath(), endsWith("serverKeystore"));
    assertThat(keyStoreConfiguration.getPassword(), is("mulepassword"));
    assertThat(keyStoreConfiguration.getKeyPassword(), is("mulepassword"));
    assertThat(keyStoreConfiguration.getAlias(), is("muleserver"));
    assertThat(keyStoreConfiguration.getType(), is(KeyStore.getDefaultType()));
    assertThat(keyStoreConfiguration.getAlgorithm(), is("PKIX"));

    TlsContextTrustStoreConfiguration trustStoreConfiguration = contextFactory.getTrustStoreConfiguration();
    assertThat(trustStoreConfiguration.getPath(), endsWith("trustStore"));
    assertThat(trustStoreConfiguration.getPassword(), is("mulepassword"));
    assertThat(trustStoreConfiguration.getType(), is("jceks"));
    assertThat(trustStoreConfiguration.getAlgorithm(), is(TrustManagerFactory.getDefaultAlgorithm()));
  }

  @Test
  public void returnsDefaultContext() {
    assertThat(defaultFactoryMock, is(sameInstance(builder.buildDefault())));
  }

  @Test
  public void failsWhenFileNotFound() throws Exception {
    expectedException.expect(CreateException.class);
    expectedException.expectMessage("Resource aPath could not be found");
    builder.trustStorePath("aPath").build();
  }

  @Test
  public void failsWhenStorePasswordIsWrong() throws Exception {
    expectedException.expect(CreateException.class);
    expectedException.expectMessage("Unable to initialise TLS configuration");
    builder.keyStorePath("serverKeystore").keyStorePassword("zaraza").keyPassword("mulepassword").build();
  }

  @Test
  public void failsWhenKeyPasswordIsWrong() throws Exception {
    expectedException.expect(CreateException.class);
    expectedException.expectMessage("Unable to initialise TLS configuration");
    builder.keyStorePath("serverKeystore").keyStorePassword("mulepassword").keyPassword("zaraza").build();
  }

}
