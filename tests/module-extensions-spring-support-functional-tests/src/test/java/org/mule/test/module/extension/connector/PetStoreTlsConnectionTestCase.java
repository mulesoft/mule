/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class PetStoreTlsConnectionTestCase extends AbstractExtensionFunctionalTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{"global tls", "globalTls"}, {"inline tls", "inlineTls"}});
  }

  private String name;
  private String configName;

  @Rule
  public SystemProperty systemProperty;

  public PetStoreTlsConnectionTestCase(String name, String configName) {
    this.name = name;
    this.configName = configName;
    systemProperty = new SystemProperty("config", configName);
  }

  @Override
  protected String getConfigFile() {
    return "petstore-tls-connection.xml";
  }

  @Test
  public void tls() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getClient").getMessage().getPayload().getValue();
    assertThat(client.getTlsContext(), is(notNullValue()));
  }
}
