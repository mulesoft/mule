/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.listener.PlatformManagedOAuthStateListener;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlatformClientCredentialsOAuthStateAdapterTest {

  @Mock
  private PlatformManagedOAuthDancer dancer;

  @Mock
  private Consumer<ResourceOwnerOAuthContext> onUpdate;

  private PlatformClientCredentialsOAuthStateAdapter adapter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testAccessTokenIsUpdatedOnTokenReceived() {
    ResourceOwnerOAuthContext context = mock(ResourceOwnerOAuthContext.class);
    when(context.getAccessToken()).thenReturn("initialToken");
    when(context.getExpiresIn()).thenReturn("3600");
    when(dancer.getContext()).thenReturn(context);

    adapter = new PlatformClientCredentialsOAuthStateAdapter(dancer, onUpdate);
    assertThat(adapter.getAccessToken(), is("initialToken"));
    assertThat(adapter.getExpiresIn().isPresent(), is(true));
    assertThat(adapter.getExpiresIn().get(), is("3600"));

    ResourceOwnerOAuthContext updatedContext = mock(ResourceOwnerOAuthContext.class);
    when(updatedContext.getAccessToken()).thenReturn("newToken");
    when(updatedContext.getExpiresIn()).thenReturn("7200");

    ArgumentCaptor<PlatformManagedOAuthStateListener> listenerCaptor =
        ArgumentCaptor.forClass(PlatformManagedOAuthStateListener.class);
    verify(dancer).addListener(listenerCaptor.capture());

    PlatformManagedOAuthStateListener listener = listenerCaptor.getValue();
    listener.onAccessToken(updatedContext);

    assertThat(adapter.getAccessToken(), is("newToken"));
    assertThat(adapter.getExpiresIn().isPresent(), is(true));
    assertThat(adapter.getExpiresIn().get(), is("7200"));

    verify(onUpdate).accept(updatedContext);
  }

  @Test
  public void testAccessTokenIsUpdatedOnTokenRefresh() {
    ResourceOwnerOAuthContext context = mock(ResourceOwnerOAuthContext.class);
    when(context.getAccessToken()).thenReturn("initialToken");
    when(context.getExpiresIn()).thenReturn("3600");
    when(dancer.getContext()).thenReturn(context);

    adapter = new PlatformClientCredentialsOAuthStateAdapter(dancer, onUpdate);
    assertThat(adapter.getAccessToken(), is("initialToken"));
    assertThat(adapter.getExpiresIn().isPresent(), is(true));
    assertThat(adapter.getExpiresIn().get(), is("3600"));

    ResourceOwnerOAuthContext refreshedContext = mock(ResourceOwnerOAuthContext.class);
    when(refreshedContext.getAccessToken()).thenReturn("refreshedToken");
    when(refreshedContext.getExpiresIn()).thenReturn("1800");

    ArgumentCaptor<PlatformManagedOAuthStateListener> listenerCaptor =
        ArgumentCaptor.forClass(PlatformManagedOAuthStateListener.class);
    verify(dancer).addListener(listenerCaptor.capture());

    PlatformManagedOAuthStateListener listener = listenerCaptor.getValue();
    listener.onTokenRefreshed(refreshedContext);

    assertThat(adapter.getAccessToken(), is("refreshedToken"));
    assertThat(adapter.getExpiresIn().isPresent(), is(true));
    assertThat(adapter.getExpiresIn().get(), is("1800"));

    verify(onUpdate).accept(refreshedContext);
  }
}
