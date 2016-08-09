/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextKeyStoreConfiguration;
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PetStoreTlsConfigTestCase extends ExtensionFunctionalTestCase {

  private static final String PASSWORD = "changeit";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{"global tls", "globalTls"}, {"inline tls", "inlineTls"}});
  }

  private String name;
  private String configName;

  @Rule
  public SystemProperty systemProperty;

  public PetStoreTlsConfigTestCase(String name, String configName) {
    this.name = name;
    this.configName = configName;
    systemProperty = new SystemProperty("config", configName);
  }

  @Override
  protected String getConfigFile() {
    return "petstore-tls-config.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Test
  public void tls() throws Exception {
    PetStoreConnector connector = getConfigurationFromRegistry(configName, getTestEvent(""));
    TlsContextFactory tls = connector.getTlsContext();
    assertThat(tls, is(notNullValue()));

    TlsContextTrustStoreConfiguration trustStoreConfig = tls.getTrustStoreConfiguration();
    assertThat(trustStoreConfig.getPath().endsWith("ssltest-cacerts.jks"), is(true));
    assertThat(trustStoreConfig.getPassword(), equalTo(PASSWORD));

    TlsContextKeyStoreConfiguration keyStoreConfiguration = tls.getKeyStoreConfiguration();
    assertThat(keyStoreConfiguration.getPath().endsWith("ssltest-keystore.jks"), is(true));
    assertThat(keyStoreConfiguration.getKeyPassword(), equalTo(PASSWORD));
    assertThat(keyStoreConfiguration.getPassword(), equalTo(PASSWORD));
  }
}
