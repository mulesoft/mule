/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.mule.runtime.extension.api.security.CredentialsPlacement.BASIC_AUTH_HEADER;

import static java.nio.charset.Charset.defaultCharset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mule.oauth.client.api.ClientCredentialsOAuthDancer;
import org.mule.oauth.client.api.listener.ClientCredentialsListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ClientCredentialsOAuthHandlerTestCase {

  @Mock
  private ClientCredentialsConfig mockConfig;

  @Mock
  private ClientCredentialsOAuthDancer mockDancer;

  @Mock
  private ResourceOwnerOAuthContext mockContext;

  @Mock
  private OAuthClientCredentialsDancerBuilder mockDancerBuilder;

  @Mock
  private ClientCredentialsGrantType mockGrantType;

  @Mock
  private LazyValue<OAuthService> mockOAuthService;

  @Mock
  private OAuthService mockOAuthServiceInstance;

  @Mock
  private ArtifactEncoding artifactEncoding;

  @InjectMocks
  private ClientCredentialsOAuthHandler handler = spy(new ClientCredentialsOAuthHandler());

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(artifactEncoding.getDefaultEncoding()).thenReturn(defaultCharset());

    when(mockConfig.getConfigIdentifier()).thenReturn("config-id");
    when(mockConfig.getOwnerConfigName()).thenReturn("owner-config");
    when(mockConfig.getTokenUrl()).thenReturn("url");
    when(mockDancer.getContext()).thenReturn(mockContext);

    when(mockConfig.getGrantType()).thenReturn(mockGrantType);
    when(mockConfig.getClientId()).thenReturn("client");
    when(mockConfig.getClientSecret()).thenReturn("secret");
    when(mockConfig.getCustomHeaders()).thenReturn(new MultiMap<>());
    when(mockConfig.getCustomBodyParameters()).thenReturn(new MultiMap<>());
    when(mockGrantType.getExpirationRegex()).thenReturn("expires_in");
    when(mockGrantType.getAccessTokenExpr()).thenReturn("access_token");
    when(mockGrantType.getCredentialsPlacement()).thenReturn(BASIC_AUTH_HEADER);
    when(mockGrantType.getDefaultScopes()).thenReturn(Optional.of("scope"));
    when(handler.getOAuthService()).thenReturn(mockOAuthService);
    when(mockOAuthService.get()).thenReturn(mockOAuthServiceInstance);
    when(mockOAuthServiceInstance.clientCredentialsGrantTypeDancerBuilder(
                                                                          any(), any(), any()))
                                                                              .thenReturn(mockDancerBuilder);

    when(mockDancerBuilder.name(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.encoding(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.clientCredentials(anyString(), anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.tokenUrl(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseExpiresInExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseAccessTokenExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.withClientCredentialsIn(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.scopes(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customParameters(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customHeaders(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customBodyParameters(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customParametersExtractorsExprs(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.build()).thenReturn(mockDancer);

    handler.getDancers().put("config-id", mockDancer);
  }

  @Test
  public void testRegister() throws Exception {
    handler.getDancers().remove("config-id");
    ClientCredentialsListener listener = mock(ClientCredentialsListener.class);
    List<ClientCredentialsListener> listeners = Collections.singletonList(listener);

    ClientCredentialsOAuthDancer result = handler.register(mockConfig, listeners);

    assertThat(result, notNullValue());
    verify(handler, times(1)).register(mockConfig, listeners);
    verify(mockDancerBuilder, times(1)).build();
  }

  @Test
  public void testRefreshToken() {
    doReturn(mock(CompletableFuture.class)).when(mockDancer).refreshToken();

    handler.refreshToken(mockConfig);

    verify(mockDancer, times(1)).refreshToken();
  }

  @Test(expected = MuleRuntimeException.class)
  public void testRefreshTokenFailure() throws Exception {
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockDancer.refreshToken()).thenReturn(future);

    doThrow(new ExecutionException("Token refresh failed", new Throwable()))
        .when(future).get();

    handler.refreshToken(mockConfig);
  }

  @Test
  public void testGetOAuthContext() throws Exception {
    CompletableFuture<String> future = mock(CompletableFuture.class);
    when(mockDancer.accessToken()).thenReturn(future);
    when(future.get()).thenReturn(any());

    ResourceOwnerOAuthContext context = handler.getOAuthContext(mockConfig);

    assertThat(context, notNullValue());
    verify(mockDancer, times(2)).getContext();
  }

  @Test
  public void testGetOAuthContextSuccess() throws Exception {
    when(mockContext.getAccessToken()).thenReturn("access-token");

    ResourceOwnerOAuthContext context = handler.getOAuthContext(mockConfig);

    assertThat(context, notNullValue());
    verify(mockDancer, times(1)).getContext();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetOAuthContextUnregistered() {
    handler.getDancers().clear();

    handler.getOAuthContext(mockConfig);
  }

  @Test(expected = MuleRuntimeException.class)
  public void testGetOAuthContextFailure() throws Exception {
    CompletableFuture<String> future = mock(CompletableFuture.class);
    when(mockDancer.accessToken()).thenReturn(future);

    doThrow(new ExecutionException("Token refresh failed", new Throwable()))
        .when(future).get();

    handler.getOAuthContext(mockConfig);
  }

  @Test
  public void testInvalidate() {
    handler.invalidate(mockConfig);

    verify(mockDancer, times(1)).invalidateContext();
  }

  @Test
  public void testInvalidateUnregistered() {
    handler.getDancers().clear();

    handler.invalidate(mockConfig);

    // Ensure no exceptions are thrown and no interaction with dancer occurs
    verify(mockDancer, times(0)).invalidateContext();
  }

  @Test
  public void testRegisterOverloaded() {
    ClientCredentialsOAuthDancer clientCredentialsOAuthDancer = handler.register(mockConfig);
    assertThat(clientCredentialsOAuthDancer, notNullValue());
  }

  @Test
  public void testGenerateId() {
    assertThat(handler.generateId(mockConfig), notNullValue());
  }
}
