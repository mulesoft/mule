/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.mule.runtime.extension.api.security.CredentialsPlacement.BASIC_AUTH_HEADER;

import static java.nio.charset.Charset.defaultCharset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.AuthorizationCodeOAuthDancer;
import org.mule.oauth.client.api.AuthorizationCodeRequest;
import org.mule.oauth.client.api.builder.AuthorizationCodeDanceCallbackContext;
import org.mule.oauth.client.api.listener.AuthorizationCodeListener;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.http.api.HttpConstants;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerFactory;
import org.mule.runtime.http.api.server.ServerAddress;
import org.mule.runtime.http.api.server.ServerNotFoundException;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthorizationCodeOAuthHandlerTestCase {

  private static final String CALLBACK_IP = "0.0.0.0";
  private static final String CALLBACK_PATH = "/callback-path";
  private static final int CALLBACK_PORT = 80;

  @Mock
  private AuthorizationCodeConfig mockConfig;

  @Mock
  private AuthorizationCodeOAuthDancer mockDancer;

  @Mock
  private OAuthAuthorizationCodeDancerBuilder mockDancerBuilder;

  @Mock
  private AuthorizationCodeGrantType mockGrantType;

  @Mock
  private LazyValue<OAuthService> mockOAuthService;

  @Mock
  private OAuthService mockOAuthServiceInstance;

  @Mock
  private ArtifactEncoding artifactEncoding;

  @Mock
  private LazyValue<HttpService> mockHttpServiceInstance;

  @Mock
  private HttpService mockHttpService;

  @Mock
  private HttpServerFactory mockHttpServerFactory;

  @Mock
  private HttpServer mockHttpServer;

  @Mock
  private OAuthCallbackConfig mockOAuthCallbackConfig;

  @Mock
  private ResourceOwnerOAuthContext mockResourceOwnerOAuthContext;

  @Mock
  private Registry registry;

  @InjectMocks
  private AuthorizationCodeOAuthHandler handler = spy(new AuthorizationCodeOAuthHandler());


  @Before
  public void setUp() throws ServerNotFoundException {
    MockitoAnnotations.initMocks(this);

    when(artifactEncoding.getDefaultEncoding()).thenReturn(defaultCharset());

    when(mockConfig.getOwnerConfigName()).thenReturn("owner-config");
    when(mockConfig.getAccessTokenUrl()).thenReturn("url");
    when(mockConfig.getConsumerKey()).thenReturn("key");
    when(mockConfig.getConsumerSecret()).thenReturn("secret");
    when(mockConfig.getGrantType()).thenReturn(mockGrantType);
    when(mockConfig.getCallbackConfig()).thenReturn(mockOAuthCallbackConfig);
    when(mockConfig.getAuthorizationUrl()).thenReturn("auth-url");
    MultiMap<String, String> testMap = new MultiMap<>();
    when(mockConfig.getCustomQueryParameters()).thenReturn(testMap);
    when(mockConfig.getCustomHeaders()).thenReturn(testMap);
    when(mockConfig.getCustomBodyParameters()).thenReturn(testMap);
    Map<Field, String> parameterExtractorsMap = new HashMap<>();
    when(mockConfig.getParameterExtractors()).thenReturn(parameterExtractorsMap);
    when(mockConfig.getResourceOwnerId()).thenReturn("resource-id");
    when(mockConfig.getScope()).thenReturn(Optional.of("scope1"));

    when(handler.getOAuthService()).thenReturn(mockOAuthService);
    when(mockOAuthService.get()).thenReturn(mockOAuthServiceInstance);
    when(mockOAuthServiceInstance.authorizationCodeGrantTypeDancerBuilder(any(), any(), any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.name(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.encoding(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.clientCredentials(anyString(), anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.tokenUrl(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseExpiresInExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseRefreshTokenExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.responseAccessTokenExpr(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.resourceOwnerIdTransformer(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.withClientCredentialsIn(any())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.externalCallbackUrl(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.authorizationUrl(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.localCallback(any(), anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.localAuthorizationUrlPath(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.localAuthorizationUrlResourceOwnerId(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.state(anyString())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customParameters(anyMap())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customHeaders(anyMap())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customBodyParameters(anyMap())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.customParametersExtractorsExprs(null)).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.includeRedirectUriInRefreshTokenRequest(anyBoolean())).thenReturn(mockDancerBuilder);
    when(mockDancerBuilder.build()).thenReturn(mockDancer);

    when(mockGrantType.getExpirationRegex()).thenReturn("expires_in");
    when(mockGrantType.getRefreshTokenExpr()).thenReturn("expires_in");
    when(mockGrantType.getAccessTokenExpr()).thenReturn("expires_in");
    when(mockGrantType.getCredentialsPlacement()).thenReturn(BASIC_AUTH_HEADER);
    when(mockOAuthCallbackConfig.getListenerConfig()).thenReturn("listener-config");
    when(mockOAuthCallbackConfig.getExternalCallbackUrl()).thenReturn(Optional.of("callback/url"));
    when(mockOAuthCallbackConfig.getCallbackPath()).thenReturn(CALLBACK_PATH);
    when(mockOAuthCallbackConfig.getLocalAuthorizePath()).thenReturn("local-auth-path");

    handler.getDancers().put("configName", mockDancer);
    handler.httpService = mockHttpServiceInstance;
    when(mockHttpServiceInstance.get()).thenReturn(mockHttpService);
    when(mockHttpService.getServerFactory()).thenReturn(mockHttpServerFactory);
    when(mockHttpServerFactory.lookup(anyString())).thenReturn(mockHttpServer);
  }

  @Test
  public void testRegister() {
    AuthorizationCodeListener listener = mock(AuthorizationCodeListener.class);
    List<AuthorizationCodeListener> listeners = Collections.singletonList(listener);

    AuthorizationCodeOAuthDancer result = handler.register(mockConfig, listeners);

    assertThat(result, notNullValue());
    verify(handler, times(1)).register(mockConfig, listeners);
    verify(mockDancerBuilder, times(1)).build();
  }

  @Test(expected = MuleRuntimeException.class)
  public void testRegisterFailure() throws Exception {
    AuthorizationCodeListener listener = mock(AuthorizationCodeListener.class);
    List<AuthorizationCodeListener> listeners = Collections.singletonList(listener);

    doThrow(new ServerNotFoundException("server not found"))
        .when(mockHttpServerFactory).lookup(anyString());

    handler.register(mockConfig, listeners);
  }

  @Test(expected = MuleRuntimeException.class)
  public void testGetListenerFlowsThrowsMuleRuntimeException() {
    doThrow(new IllegalArgumentException("not found")).when(handler).lookupFlow(Optional.ofNullable(any()));

    handler.getListenerFlows(mockConfig);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLookupFlow() {
    when(registry.lookupByName(anyString())).thenReturn(Optional.empty());
    handler.lookupFlow("invalid-flow");
  }

  @Test
  public void testBeforeCallback() {
    Flow mockFlow = mock(Flow.class);
    AuthorizationCodeRequest mockCodeRequest = mock(AuthorizationCodeRequest.class);
    CoreEvent mockEvent = mock(CoreEvent.class);
    doReturn(mockEvent).when(handler).runFlow(mockFlow, mockEvent, mockConfig, "before");
    doReturn(mockEvent).when(handler).createEvent(any(), any(), any());

    AuthorizationCodeDanceCallbackContext context = handler.beforeCallback(mockConfig, mockFlow).apply(mockCodeRequest);

    assertThat(context, is(notNullValue()));
    verify(handler, times(1)).runFlow(mockFlow, mockEvent, mockConfig, "before");

    Optional<Object> event = context.getParameter("event");
    assertThat(event.isPresent(), is(true));
    assertThat(event.get(), is(mockEvent));
  }

  @Test
  public void testAfterCallback() {
    Flow mockFlow = mock(Flow.class);
    AuthorizationCodeDanceCallbackContext callbackContext = mock(AuthorizationCodeDanceCallbackContext.class);
    ResourceOwnerOAuthContext oAuthContext = mock(ResourceOwnerOAuthContext.class);
    CoreEvent mockEvent = mock(InternalEvent.class);
    CaseInsensitiveHashMap mockMap = mock(CaseInsensitiveHashMap.class);
    when(mockEvent.getVariables()).thenReturn(mockMap);
    when(mockEvent.getParameters()).thenReturn(mockMap);
    doReturn(mockEvent).when(handler).runFlow(eq(mockFlow), any(), eq(mockConfig), eq("after"));
    doReturn(mockEvent).when(handler).createEvent(any(), any(), any());

    handler.afterCallback(mockConfig, mockFlow).accept(callbackContext, oAuthContext);

    verify(handler, times(1)).runFlow(eq(mockFlow), any(), eq(mockConfig), eq("after"));
  }

  @Test
  public void testGetExternalCallback() {
    ServerAddress mockServerAddress = mock(ServerAddress.class);
    when(mockHttpServer.getProtocol()).thenReturn(HttpConstants.Protocol.HTTP);
    when(mockHttpServer.getServerAddress()).thenReturn(mockServerAddress);
    when(mockServerAddress.getIp()).thenReturn(CALLBACK_IP);
    when(mockServerAddress.getPort()).thenReturn(CALLBACK_PORT);
    when(mockOAuthCallbackConfig.getExternalCallbackUrl()).thenReturn(Optional.empty());

    String callback = handler.getExternalCallback(mockHttpServer, mockOAuthCallbackConfig);

    assertThat(callback, is(HttpConstants.Protocol.HTTP.getScheme() + "://" + CALLBACK_IP + ":" + CALLBACK_PORT + CALLBACK_PATH));
  }

  @Test(expected = MuleRuntimeException.class)
  public void testGetExternalCallbackFailure() {
    ServerAddress mockServerAddress = mock(ServerAddress.class);
    when(mockHttpServer.getProtocol()).thenReturn(HttpConstants.Protocol.HTTP);
    when(mockHttpServer.getServerAddress()).thenReturn(mockServerAddress);
    when(mockServerAddress.getIp()).thenReturn(CALLBACK_IP);
    when(mockServerAddress.getPort()).thenReturn(-2);
    when(mockOAuthCallbackConfig.getExternalCallbackUrl()).thenReturn(Optional.empty());

    handler.getExternalCallback(mockHttpServer, mockOAuthCallbackConfig);
  }

  @Test
  public void testInvalidate() {
    handler.invalidate("configName", "");

    verify(mockDancer, times(1)).invalidateContext(anyString(), anyBoolean());
  }

  @Test
  public void testInvalidateUnregistered() {
    handler.getDancers().clear();

    handler.invalidate("configName", "");

    // Ensure no exceptions are thrown and no interaction with dancer occurs
    verify(mockDancer, times(0)).invalidateContext(anyString(), anyBoolean());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterWithNullListeners() throws Exception {
    handler.register(mockConfig, null);
  }

  @Test
  public void testRegisterOverloaded() {
    AuthorizationCodeOAuthDancer dancer = handler.register(mockConfig);
    assertThat(dancer, notNullValue());
  }

  @Test
  public void testRefreshToken() {
    doReturn(mock(CompletableFuture.class)).when(mockDancer).refreshToken("ownerID");

    handler.refreshToken("configName", "ownerID");

    verify(mockDancer, times(1)).refreshToken("ownerID");
  }

  @Test(expected = MuleRuntimeException.class)
  public void testRefreshTokenFailure() throws Exception {
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockDancer.refreshToken("ownerID")).thenReturn(future);

    doThrow(new ExecutionException("Token refresh failed", new Throwable()))
        .when(future).get();

    handler.refreshToken("configName", "ownerID");
  }

  @Test
  public void testGetOAuthContextSuccess() throws Exception {
    when(mockDancer.getContextForResourceOwner(anyString())).thenReturn(mockResourceOwnerOAuthContext);
    when(mockResourceOwnerOAuthContext.getAccessToken()).thenReturn("access_token");

    handler.getDancers().put("owner-config", mockDancer);

    Optional<ResourceOwnerOAuthContext> context = handler.getOAuthContext(mockConfig);

    assertThat(context, notNullValue());
  }

  @Test
  public void testGetOAuthContextNullDancer() {
    when(mockDancer.getContextForResourceOwner(anyString())).thenReturn(mockResourceOwnerOAuthContext);

    Optional<ResourceOwnerOAuthContext> context = handler.getOAuthContext(mockConfig);

    assertThat(context.isPresent(), is(false));
  }

  @Test
  public void testGetOAuthContextNullAccessToken() {
    when(mockDancer.getContextForResourceOwner(anyString())).thenReturn(mockResourceOwnerOAuthContext);

    handler.getDancers().put("owner-config", mockDancer);

    Optional<ResourceOwnerOAuthContext> context = handler.getOAuthContext(mockConfig);

    assertThat(context.isPresent(), is(false));
  }
}

