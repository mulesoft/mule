/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.List;

import org.junit.Test;

public abstract class PetStoreConnectionTestCase extends AbstractExtensionFunctionalTestCase {

  protected static final String DEFAULT_CONFIG_NAME = "petstore";

  @Test
  public void getPets() throws Exception {
    ConfigurationInstance config = muleContext.getExtensionManager().getConfiguration("petstore", testEvent());
    assertThat(config, is(notNullValue()));

    CoreEvent response = runFlow("getPets");
    List<String> pets = (List<String>) response.getMessage().getPayload().getValue();
    PetStoreConnector configValue = (PetStoreConnector) config.getValue();
    assertThat(pets, containsInAnyOrder(configValue.getPets().toArray()));
  }

  @Test
  public void verifyClient() throws Exception {
    PetStoreClient client = getClient();
    assertValidClient(client);
  }

  @Test
  public void stopAndDisconnect() throws Exception {
    PetStoreClient client = getClient();
    muleContext.stop();

    assertThat(client.getDisconnectCount(), is(1));
    assertThat(client.hasActiveConnection(), is(false));
  }

  protected PetStoreClient getClient() throws Exception {
    PetStoreClient client = (PetStoreClient) runFlow("getClient").getMessage().getPayload().getValue();
    assertThat(client, is(notNullValue()));
    return client;
  }

  protected void assertValidClient(PetStoreClient client) {
    assertThat(client.getUsername(), is("john"));
    assertThat(client.getPassword(), is("doe"));

    assertConnected(client);
    assertThat(client.getConfigName(), equalTo(getConfigName()));
  }

  protected void assertConnected(PetStoreClient client) {
    assertThat(client.hasActiveConnection(), is(true));
  }

  protected String getConfigName() {
    return DEFAULT_CONFIG_NAME;
  }
}
