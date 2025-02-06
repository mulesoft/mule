/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.nio.charset.Charset.defaultCharset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.http.api.server.ServerNotFoundException;
import org.mule.runtime.module.extension.internal.loader.java.property.oauth.OAuthCallbackValuesModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConfig;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthPlatformManagedDancerBuilder;
import org.mule.runtime.oauth.api.listener.PlatformManagedOAuthStateListener;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlatformManagedOAuthHandlerTestCase {

  @Mock
  private PlatformManagedOAuthConfig mockConfig;

  @Mock
  private PlatformManagedOAuthDancer mockDancer;

  @Mock
  private OAuthPlatformManagedDancerBuilder mockDancerBuilder;

  @Mock
  private OAuthGrantType mockGrantType;

  @Mock
  private PlatformManagedOAuthGrantType mockPlatformManagedOAuthGrantType;

  @Mock
  private LazyValue<OAuthService> mockOAuthService;

  @Mock
  private OAuthService mockOAuthServiceInstance;

  @Mock
  private ArtifactEncoding artifactEncoding;

  @Mock
  private ConnectionProviderModel mockConnectionProviderModel;

  @Mock
  private OAuthCallbackValuesModelProperty mockOAuthCallbackValuesModelProperty;

  @Mock
  private ResourceOwnerOAuthContext mockResourceOwnerOAuthContext;

  @InjectMocks
  private PlatformManagedOAuthHandler handler = spy(new PlatformManagedOAuthHandler());

  @Before
  public void setUp() throws ServerNotFoundException {
    MockitoAnnotations.initMocks(this);

    when(artifactEncoding.getDefaultEncoding()).thenReturn(defaultCharset());

    when(mockConfig.getOwnerConfigName()).thenReturn("owner-config");
    when(mockConfig.getServiceUrl()).thenReturn("service-url");
    when(mockConfig.getConnectionUri()).thenReturn("connection-url");
    when(mockConfig.getOrgId()).thenReturn("orgID");
    when(mockConfig.getApiVersion()).thenReturn("version");
    when(mockConfig.getPlatformAuthUrl()).thenReturn("url");
    when(mockConfig.getClientId()).thenReturn("key");
    when(mockConfig.getClientSecret()).thenReturn("secret");
    when(mockConfig.getDelegateGrantType()).thenReturn(mockGrantType);
    when(mockConfig.getGrantType()).thenReturn(mockPlatformManagedOAuthGrantType);

    Map<Field, String> parameterExtractorsMap = new HashMap<>();
    when(mockConfig.getParameterExtractors()).thenReturn(parameterExtractorsMap);
    when(mockConfig.getDelegateConnectionProviderModel()).thenReturn(mockConnectionProviderModel);

    when(mockConnectionProviderModel.getModelProperty(any())).thenReturn(Optional.of(mockOAuthCallbackValuesModelProperty));
    Map<Field, String> oAuthCallbaclValuesMap = new HashMap<>();
    when(mockOAuthCallbackValuesModelProperty.getCallbackValues()).thenReturn(oAuthCallbaclValuesMap);

    when(handler.getOAuthService()).thenReturn(mockOAuthService);
    when(mockOAuthService.get()).thenReturn(mockOAuthServiceInstance);
    when(mockOAuthServiceInstance.platformManagedOAuthDancerBuilder(any(), any(), any())).thenReturn(mockDancerBuilder);

    when(mockDancerBuilder.connectionUri(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.platformUrl(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.organizationId(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.apiVersion(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.name(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.clientCredentials(anyString(), anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.tokenUrl(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseExpiresInExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseRefreshTokenExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.withClientCredentialsIn(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.resourceOwnerIdTransformer(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.encoding(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseAccessTokenExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customParametersExtractorsExprs(null)).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.build()).thenReturn(mockDancer);
    when(mockGrantType.getExpirationRegex()).thenReturn("expires_in");

    when(mockPlatformManagedOAuthGrantType.getAccessTokenExpr()).thenReturn("expires_in");
  }

  @Test
  public void testRegister() {
    PlatformManagedOAuthStateListener listener = mock(PlatformManagedOAuthStateListener.class);
    List<PlatformManagedOAuthStateListener> listeners = Collections.singletonList(listener);

    PlatformManagedOAuthDancer result = handler.register(mockConfig, listeners);

    assertThat(result, notNullValue());
    verify(handler, times(1)).register(mockConfig, listeners);
    verify(mockDancerBuilder, times(1)).build();
  }

  @Test
  public void testRegisterOverloaded() {
    PlatformManagedOAuthDancer dancer = handler.register(mockConfig);
    assertThat(dancer, notNullValue());
  }

  @Test
  public void testInvalidate() {
    handler.getDancers().put("owner-config", mockDancer);
    handler.invalidate(mockConfig);

    verify(mockDancer, times(1)).invalidateContext();
  }

  @Test
  public void testInvalidateUnregistered() {
    handler.invalidate(mockConfig);

    // Ensure no exceptions are thrown and no interaction with dancer occurs
    verify(mockDancer, times(0)).invalidateContext();
  }

  @Test
  public void testRefreshToken() {
    handler.getDancers().put("owner-config", mockDancer);
    doReturn(mock(CompletableFuture.class)).when(mockDancer).refreshToken();
    handler.refreshToken(mockConfig);

    verify(mockDancer, times(1)).refreshToken();
  }

  @Test(expected = MuleRuntimeException.class)
  public void testRefreshTokenFailure() throws Exception {
    handler.getDancers().put("owner-config", mockDancer);
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockDancer.refreshToken()).thenReturn(future);

    doThrow(new ExecutionException("Token refresh failed", new Throwable()))
        .when(future).get();

    handler.refreshToken(mockConfig);
  }

  @Test
  public void testGetOAuthContextSuccess() throws Exception {
    when(mockDancer.getContext()).thenReturn(mockResourceOwnerOAuthContext);
    when(mockResourceOwnerOAuthContext.getAccessToken()).thenReturn("access_token");
    handler.getDancers().put("owner-config", mockDancer);
    CompletableFuture<String> mockAccessTokenCompletableFuture = mock(CompletableFuture.class);
    when(mockDancer.accessToken()).thenReturn(mockAccessTokenCompletableFuture);
    ResourceOwnerOAuthContext context = handler.getOAuthContext(mockConfig);

    assertThat(context, notNullValue());
  }

  @Test(expected = IllegalStateException.class)
  public void testGetOAuthContextNullDancer() {
    when(mockDancer.getContext()).thenReturn(mockResourceOwnerOAuthContext);

    handler.getOAuthContext(mockConfig);
  }

  @Test
  public void testGetOAuthContextNullContextForResourceOwner() throws Exception {
    handler.getDancers().put("owner-config", mockDancer);
    CompletableFuture<String> mockAccessTokenCompletableFuture = mock(CompletableFuture.class);
    when(mockDancer.accessToken()).thenReturn(mockAccessTokenCompletableFuture);
    ResourceOwnerOAuthContext context = handler.getOAuthContext(mockConfig);

    assertThat(context, is(nullValue()));
  }

  @Test(expected = MuleRuntimeException.class)
  public void testGetOAuthContextNullAccessToken() throws ExecutionException, InterruptedException {
    when(mockDancer.getContext()).thenReturn(mockResourceOwnerOAuthContext);
    CompletableFuture<String> mockAccessTokenCompletableFuture = mock(CompletableFuture.class);
    when(mockDancer.accessToken()).thenReturn(mockAccessTokenCompletableFuture);
    doThrow(new InterruptedException("exception")).when(mockAccessTokenCompletableFuture).get();
    handler.getDancers().put("owner-config", mockDancer);

    ResourceOwnerOAuthContext context = handler.getOAuthContext(mockConfig);

    assertThat(context, is(nullValue()));
  }

  @Test
  public void testBuildObjectStoreLocator() {
    Function<OAuthConfig, ObjectStore> func = handler.buildObjectStoreLocator();
    assertThat(func, notNullValue());
  }
}
