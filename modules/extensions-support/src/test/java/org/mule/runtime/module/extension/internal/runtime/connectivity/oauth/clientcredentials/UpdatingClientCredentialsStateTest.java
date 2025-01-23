/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.mule.oauth.client.api.ClientCredentialsOAuthDancer;
import org.mule.oauth.client.api.listener.ClientCredentialsListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.exception.TokenInvalidatedException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UpdatingClientCredentialsStateTest {

  private ClientCredentialsOAuthDancer dancer;
  private ResourceOwnerOAuthContext initialContext;
  private Consumer<ResourceOwnerOAuthContext> onUpdate;
  private UpdatingClientCredentialsState updatingClientCredentialsState;

  @Before
  public void setUp() {
    dancer = mock(ClientCredentialsOAuthDancer.class);
    initialContext = mock(ResourceOwnerOAuthContext.class);
    onUpdate = mock(Consumer.class);

    when(initialContext.getAccessToken()).thenReturn("initialAccessToken");
    when(initialContext.getExpiresIn()).thenReturn("3600");

    updatingClientCredentialsState = new UpdatingClientCredentialsState(dancer, initialContext, onUpdate);
  }

  @Test
  public void testGetAccessTokenWhenNotInvalidated() {
    String accessToken = updatingClientCredentialsState.getAccessToken();

    assertEquals("initialAccessToken", accessToken);
  }

  @Test
  public void testGetAccessTokenWhenInvalidatedAndTokenRefreshed() throws Exception {
    CompletableFuture<String> future = mock(CompletableFuture.class);
    when(dancer.accessToken()).thenReturn(future);

    // Simulate token invalidation
    ArgumentCaptor<ClientCredentialsListener> listenerCaptor = ArgumentCaptor.forClass(ClientCredentialsListener.class);
    verify(dancer).addListener(listenerCaptor.capture());
    ClientCredentialsListener listener = listenerCaptor.getValue();

    listener.onTokenInvalidated();

    when(dancer.getContext()).thenReturn(initialContext);
    when(initialContext.getAccessToken()).thenReturn("newAccessToken");

    String accessToken = updatingClientCredentialsState.getAccessToken();
    assertEquals("newAccessToken", accessToken);
  }

  @Test(expected = TokenInvalidatedException.class)
  public void testGetAccessTokenWhenInvalidatedAndTokenRefreshFails() throws Exception {
    when(dancer.accessToken()).thenThrow(new RuntimeException("Refresh failed"));

    // Simulate token invalidation
    ArgumentCaptor<ClientCredentialsListener> listenerCaptor = ArgumentCaptor.forClass(ClientCredentialsListener.class);
    verify(dancer).addListener(listenerCaptor.capture());
    ClientCredentialsListener listener = listenerCaptor.getValue();

    listener.onTokenInvalidated();

    updatingClientCredentialsState.getAccessToken();
  }

  @Test
  public void testOnTokenRefreshed() {
    ResourceOwnerOAuthContext updatedContext = mock(ResourceOwnerOAuthContext.class);
    when(updatedContext.getAccessToken()).thenReturn("updatedAccessToken");
    when(updatedContext.getExpiresIn()).thenReturn("7200");

    // Simulate token refresh
    ArgumentCaptor<ClientCredentialsListener> listenerCaptor = ArgumentCaptor.forClass(ClientCredentialsListener.class);
    verify(dancer).addListener(listenerCaptor.capture());
    ClientCredentialsListener listener = listenerCaptor.getValue();

    listener.onTokenRefreshed(updatedContext);

    verify(onUpdate).accept(updatedContext);
    assertEquals("updatedAccessToken", updatingClientCredentialsState.getAccessToken());
  }

  @Test
  public void testDeregisterListener() {
    updatingClientCredentialsState.deregisterListener();

    verify(dancer).removeListener(any(ClientCredentialsListener.class));
  }

  @Test
  public void testGetExpiresIn() {
    assertNotNull(updatingClientCredentialsState.getExpiresIn());
  }
}

