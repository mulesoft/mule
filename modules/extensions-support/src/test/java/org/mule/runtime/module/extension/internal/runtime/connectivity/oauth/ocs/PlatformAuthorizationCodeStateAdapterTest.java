/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.oauth.api.PlatformManagedConnectionDescriptor;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;

public class PlatformAuthorizationCodeStateAdapterTest {

  @Mock
  private PlatformManagedOAuthDancer dancer;

  @Mock
  private PlatformManagedConnectionDescriptor descriptor;

  @Mock
  private Consumer<ResourceOwnerOAuthContext> onUpdate;

  private PlatformAuthorizationCodeStateAdapter adapter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(descriptor.getDisplayName()).thenReturn("TestOwner");
    ResourceOwnerOAuthContext context = mock(ResourceOwnerOAuthContext.class);
    when(context.getAccessToken()).thenReturn("initialToken");
    when(context.getExpiresIn()).thenReturn("3600");
    when(dancer.getContext()).thenReturn(context);
    adapter = new PlatformAuthorizationCodeStateAdapter(dancer, descriptor, onUpdate);
  }

  @Test
  public void testGetRefreshTokenReturnsEmpty() {
    Optional<String> refreshToken = adapter.getRefreshToken();
    assertThat(refreshToken.isPresent(), is(false));
  }

  @Test
  public void testGetResourceOwnerIdReturnsDescriptorDisplayName() {
    String resourceOwnerId = adapter.getResourceOwnerId();
    assertThat(resourceOwnerId, is("TestOwner"));
  }

  @Test
  public void testGetStateReturnsEmpty() {
    Optional<String> state = adapter.getState();
    assertThat(state.isPresent(), is(false));
  }

  @Test
  public void testGetAuthorizationUrlReturnsEmptyString() {
    String authorizationUrl = adapter.getAuthorizationUrl();
    assertThat(authorizationUrl, is(emptyString()));
  }

  @Test
  public void testGetAccessTokenUrlReturnsEmptyString() {
    String accessTokenUrl = adapter.getAccessTokenUrl();
    assertThat(accessTokenUrl, is(emptyString()));
  }

  @Test
  public void testGetConsumerKeyReturnsEmptyString() {
    String consumerKey = adapter.getConsumerKey();
    assertThat(consumerKey, is(emptyString()));
  }

  @Test
  public void testGetConsumerSecretReturnsEmptyString() {
    String consumerSecret = adapter.getConsumerSecret();
    assertThat(consumerSecret, is(emptyString()));
  }

  @Test
  public void testGetExternalCallbackUrlReturnsEmpty() {
    Optional<String> callbackUrl = adapter.getExternalCallbackUrl();
    assertThat(callbackUrl.isPresent(), is(false));
  }
}
