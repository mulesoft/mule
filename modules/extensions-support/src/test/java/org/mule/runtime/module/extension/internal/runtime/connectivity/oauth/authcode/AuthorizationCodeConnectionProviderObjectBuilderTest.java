/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.CALLBACK_PATH_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.EXTERNAL_CALLBACK_URL_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.LISTENER_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.LOCAL_AUTHORIZE_PATH_PARAMETER_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_AUTHORIZATION_CODE_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_CALLBACK_GROUP_NAME;

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
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;

import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuthorizationCodeConnectionProviderObjectBuilderTest {

  private AuthorizationCodeConnectionProviderObjectBuilder<Object> builder;

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
  private AuthorizationCodeOAuthHandler authorizationCodeOAuthHandler;

  @Mock
  private AuthorizationCodeGrantType grantType;

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
  private ConnectionProviderFactory connectionProviderFactory;

  @Mock
  private TestConnectionProvider connectionProvider;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(providerModel.getModelProperty(ConnectionProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(new ConnectionProviderFactoryModelProperty(connectionProviderFactory)));
    when(connectionProviderFactory.newInstance()).thenReturn(connectionProvider);

    builder = new AuthorizationCodeConnectionProviderObjectBuilder<>(
                                                                     providerModel,
                                                                     resolverSet,
                                                                     poolingProfile,
                                                                     reconnectionConfig,
                                                                     grantType,
                                                                     authorizationCodeOAuthHandler,
                                                                     extensionModel,
                                                                     expressionManager,
                                                                     muleContext);
  }

  @Test
  public void testDoBuild() throws Exception {
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

    Map<String, Object> configMap = Map.of(LISTENER_CONFIG_PARAMETER_NAME, "", CALLBACK_PATH_PARAMETER_NAME, "path",
                                           LOCAL_AUTHORIZE_PATH_PARAMETER_NAME, "path", EXTERNAL_CALLBACK_URL_PARAMETER_NAME, "");
    when(resolverSetResult.get(OAUTH_CALLBACK_GROUP_NAME)).thenReturn(configMap);
    when(resolverSetResult.get(OAUTH_AUTHORIZATION_CODE_GROUP_NAME)).thenReturn(mock(Map.class));
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);


    ConnectionProvider<Object> provider = builder.doBuild(resolverSetResult);

    assertThat(provider, is(notNullValue()));
    assertThat(provider, isA(AuthorizationCodeConnectionProviderWrapper.class));
  }

  @Test
  public void testBuild() throws Exception {
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getDefaultEncoding()).thenReturn("encoding");
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(resolverSet.resolve(valueResolvingContext)).thenReturn(resolverSetResult);
    Map<String, Object> configMap = Map.of(LISTENER_CONFIG_PARAMETER_NAME, "", CALLBACK_PATH_PARAMETER_NAME, "path",
                                           LOCAL_AUTHORIZE_PATH_PARAMETER_NAME, "path", EXTERNAL_CALLBACK_URL_PARAMETER_NAME, "");
    when(valueResolver.resolve(any())).thenReturn(configMap);
    when(resolverSet.getResolvers())
        .thenReturn(Map.of(OAUTH_AUTHORIZATION_CODE_GROUP_NAME, valueResolver, OAUTH_CALLBACK_GROUP_NAME, valueResolver));


    Pair<ConnectionProvider<Object>, ResolverSetResult> result = builder.build(valueResolvingContext);

    assertThat(result, is(notNullValue()));
    assertThat(result.getFirst(), is(notNullValue()));
    assertThat(result.getFirst(), isA(AuthorizationCodeConnectionProviderWrapper.class));
    assertThat(result.getSecond(), is(resolverSetResult));
  }

  class TestConnectionProvider implements ConnectionProvider {

    AuthorizationCodeState authorizationCodeState;

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
