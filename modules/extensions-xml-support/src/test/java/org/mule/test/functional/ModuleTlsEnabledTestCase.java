/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.runtime.core.api.util.FileUtils.getResourcePath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.test.heisenberg.extension.HeisenbergConnection;
import org.mule.test.petstore.extension.PetStoreClient;

import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;

public class ModuleTlsEnabledTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String[] getModulePaths() {
    return new String[] {"modules/module-tls-config.xml", "modules/module-tls-config-with-default.xml",
        "modules/module-tls-config-required.xml", "modules/module-tls-config-required-with-default.xml"};
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-tls-config.xml";
  }

  @Override
  protected boolean shouldValidateXml() {
    return true;
  }

  @Test
  public void noTlsContextProvided() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getPetStoreClientNoTls").getMessage().getPayload().getValue();
    assertThat(client.getTlsContext(), is(nullValue()));
  }

  @Test
  public void whenInnerConfigDoesNotEnableTlsThenItIsNotMacroExpanded() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getPetStoreClientNoCustomTlsSupport").getMessage().getPayload().getValue();
    assertThat(client.getTlsContext(), is(nullValue()));
  }

  @Test
  public void tlsContextByRef() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getPetStoreClientByRef").getMessage().getPayload().getValue();
    assertExpectedTlsContext(client.getTlsContext());
  }

  @Test
  public void tlsContextInline() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getPetStoreClientInline").getMessage().getPayload().getValue();
    assertExpectedTlsContext(client.getTlsContext());
  }

  @Test
  public void tlsContextRequired() throws Exception {
    HeisenbergConnection connection =
        (HeisenbergConnection) runFlow("getHeisenbergConnectionRequiredByRef").getMessage().getPayload().getValue();

    assertExpectedTlsContext(connection.getTlsContextFactory());
  }

  @Test
  public void noCustomTlsButDefaultFromModule() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getPetStoreClientWithDefault").getMessage().getPayload().getValue();

    // The given config does not provide a TLS context but the config in the module definition already has one
    TlsContextFactory actualTlsContextFactory = client.getTlsContext();
    assertThat(actualTlsContextFactory, is(notNullValue()));
    assertThat(actualTlsContextFactory.getTrustStoreConfiguration().getPassword(), is("changeit2"));
  }

  @Test
  public void whenCustomTlsIsProvidedToTlsEnabledThenItTakesPrecedence() throws Exception {
    // Even when the module's internal element already had a TLS context, since it was marked as tlsEnabled, it can be overridden
    // by the TLS config given to the module's config
    PetStoreClient client = (PetStoreClient) runFlow("getPetStoreClientWithDefaultByRef").getMessage().getPayload().getValue();
    assertExpectedTlsContext(client.getTlsContext());
  }

  @Test
  public void whenInnerConfigDoesNotEnableTlsAndHasDefaultThenDefaultTakesPrecedence() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getPetStoreClientFixedTls").getMessage().getPayload().getValue();

    // The given config provides a TLS context but the config in the module definition already has one, and it is not marked as
    // tlsEnabled
    TlsContextFactory actualTlsContextFactory = client.getTlsContext();
    assertThat(actualTlsContextFactory, is(notNullValue()));
    assertThat(actualTlsContextFactory.getTrustStoreConfiguration().getPassword(), is("changeit2"));
  }

  @Test
  public void whenInnerConfigRequiresTlsButProvidesOneThenBecomesOptional() throws Exception {
    HeisenbergConnection connection =
        (HeisenbergConnection) runFlow("getHeisenbergConnectionRequiredWithDefaultNoTls").getMessage().getPayload().getValue();

    // The given config does not provide a TLS context but the config in the module definition already has one
    TlsContextFactory actualTlsContextFactory = connection.getTlsContextFactory();
    assertThat(actualTlsContextFactory, is(notNullValue()));
    assertThat(actualTlsContextFactory.getTrustStoreConfiguration().getPassword(), is("changeit2"));
  }

  private void assertExpectedTlsContext(TlsContextFactory actualTlsContextFactory) throws IOException {
    String expectedKeyStorePath = getResourcePath("ssltest-keystore.jks", getClass());
    String expectedTrustStorePath = getResourcePath("ssltest-cacerts.jks", getClass());

    assertThat(actualTlsContextFactory, is(notNullValue()));

    // TLS Context parameters
    assertThat(actualTlsContextFactory.getEnabledProtocols(), is(arrayContaining("TLSv1.2")));
    // This one is not present and has no default value
    assertThat(actualTlsContextFactory.getEnabledCipherSuites(), is(nullValue()));

    // Key Store parameters
    assertThat(actualTlsContextFactory.getKeyStoreConfiguration().getKeyPassword(), is("changeit"));
    assertThat(actualTlsContextFactory.getKeyStoreConfiguration().getPassword(), is("changeit"));
    assertThat(actualTlsContextFactory.getKeyStoreConfiguration().getPath(), is(expectedKeyStorePath));
    // These should have the default values
    assertThat(actualTlsContextFactory.getKeyStoreConfiguration().getType(), is(KeyStore.getDefaultType()));
    assertThat(actualTlsContextFactory.getKeyStoreConfiguration().getAlgorithm(), is(KeyManagerFactory.getDefaultAlgorithm()));

    // Trust Store parameters
    assertThat(actualTlsContextFactory.getTrustStoreConfiguration().getPassword(), is("changeit"));
    assertThat(actualTlsContextFactory.getTrustStoreConfiguration().getPath(), is(expectedTrustStorePath));
    // These should have the default values
    assertThat(actualTlsContextFactory.getTrustStoreConfiguration().getType(), is(KeyStore.getDefaultType()));
    assertThat(actualTlsContextFactory.getTrustStoreConfiguration().getAlgorithm(),
               is(TrustManagerFactory.getDefaultAlgorithm()));
  }

}
