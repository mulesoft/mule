/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_CLIENT_CREDENTIALS_GROUP_NAME;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientCredentialsConnectionProviderObjectBuilderTest {


  private ClientCredentialsConnectionProviderObjectBuilder<Object> builder;

  @Mock
  private ResolverSet resolverSet;

  @Mock
  private ValueResolver valueResolver;

  @Mock
  private ConnectionProviderModel providerModel;

  @Mock
  private PoolingProfile poolingProfile;

  @Mock
  private ReconnectionConfig reconnectionConfig;

  @Mock
  private ClientCredentialsOAuthHandler clientCredentialsHandler;

  @Mock
  private ClientCredentialsGrantType grantType;

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private ExpressionManager expressionManager;

  @Mock
  private MuleContext muleContext;

  @Mock
  private ResolverSetResult resolverSetResult;

  @Mock
  private ValueResolvingContext valueResolvingContext;

  @Mock
  private CoreEvent event;

  @Mock
  private ConnectionProviderFactory connectionProviderFactory;

  @Mock
  private TestConnectionProvider connectionProvider;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(providerModel.getModelProperty(ConnectionProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(new ConnectionProviderFactoryModelProperty(connectionProviderFactory)));
    when(connectionProviderFactory.newInstance()).thenReturn(connectionProvider);

    builder = new ClientCredentialsConnectionProviderObjectBuilder<>(
                                                                     providerModel,
                                                                     resolverSet,
                                                                     poolingProfile,
                                                                     reconnectionConfig,
                                                                     grantType,
                                                                     clientCredentialsHandler,
                                                                     extensionModel,
                                                                     expressionManager,
                                                                     muleContext);
  }

  @Test
  public void testDoBuild() throws Exception {
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

    when(resolverSetResult.get(OAUTH_CLIENT_CREDENTIALS_GROUP_NAME))
        .thenReturn(Collections.singletonMap("client_id", "testClientId"));
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(muleConfiguration.getDefaultEncoding()).thenReturn("encoding");
    when(grantType.getCredentialsPlacement()).thenReturn(CredentialsPlacement.BASIC_AUTH_HEADER);

    ConnectionProvider<Object> provider = builder.doBuild(resolverSetResult);

    assertThat(provider, is(notNullValue()));
    assertThat(provider, isA(ClientCredentialsConnectionProviderWrapper.class));
  }

  @Test
  public void testBuild() throws Exception {
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

    Map<String, String> expectedParams = new HashMap<>();
    expectedParams.put("client_id", "testClientId");
    expectedParams.put("client_secret", "testClientSecret");

    when(resolverSet.resolve(valueResolvingContext)).thenReturn(resolverSetResult);
    when(resolverSet.getResolvers()).thenReturn(Collections.singletonMap(OAUTH_CLIENT_CREDENTIALS_GROUP_NAME, valueResolver));

    when(valueResolver.resolve(any())).thenReturn(expectedParams);

    when(resolverSetResult.get(OAUTH_CLIENT_CREDENTIALS_GROUP_NAME)).thenReturn(expectedParams);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(muleConfiguration.getDefaultEncoding()).thenReturn("encoding");
    when(grantType.getCredentialsPlacement()).thenReturn(CredentialsPlacement.BASIC_AUTH_HEADER);

    Pair<ConnectionProvider<Object>, ResolverSetResult> result = builder.build(valueResolvingContext);

    assertThat(result, is(notNullValue()));
    assertThat(result.getFirst(), is(notNullValue()));
    assertThat(result.getFirst(), isA(ClientCredentialsConnectionProviderWrapper.class));
    assertThat(result.getSecond(), is(resolverSetResult));
  }

  class TestConnectionProvider implements ConnectionProvider {

    ClientCredentialsState clientCredentialsState;

    @Override
    public Object connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(Object connection) {

    }

    @Override
    public ConnectionValidationResult validate(Object connection) {
      return null;
    }
  }
}

