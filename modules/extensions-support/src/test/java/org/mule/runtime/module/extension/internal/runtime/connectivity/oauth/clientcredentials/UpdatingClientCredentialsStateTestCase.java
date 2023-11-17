/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.oauth.client.api.ClientCredentialsOAuthDancer;
import org.mule.oauth.client.api.listener.ClientCredentialsListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

public class UpdatingClientCredentialsStateTestCase {

  private UpdatingClientCredentialsState updatingClientCredentialsState;
  private ClientCredentialsListener clientCredentialsListener;
  private ClientCredentialsOAuthDancer dancer;
  private ResourceOwnerOAuthContext context;
  private Consumer<ResourceOwnerOAuthContext> onUpdate;


  @Before
  public void setUp() throws Exception {
    clientCredentialsListener = mock(ClientCredentialsListener.class);
    context = mock(ResourceOwnerOAuthContext.class);
    onUpdate = mock(Consumer.class);
    // dancer =
    updatingClientCredentialsState =
        new UpdatingClientCredentialsState(dancer, context, onUpdate);
    dancer.addListener(clientCredentialsListener);
  }

  @Test
  public void deRegisterListener() {
    assertThat(updatingClientCredentialsState.getListener(clientCredentialsListener), is(true));
    updatingClientCredentialsState.deRegisterListener();
    assertThat(updatingClientCredentialsState.getListener(clientCredentialsListener), is(false));
  }
}
