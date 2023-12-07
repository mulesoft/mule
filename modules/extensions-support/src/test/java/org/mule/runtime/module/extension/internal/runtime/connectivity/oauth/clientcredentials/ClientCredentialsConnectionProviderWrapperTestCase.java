/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.mule.runtime.extension.api.security.CredentialsPlacement.BODY;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.OauthFeature.SDK_OAUTH_SUPPORT;

import org.mule.oauth.client.api.ClientCredentialsOAuthDancer;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

import org.junit.Test;

@Issue("W-14391247")
@Feature(SDK_OAUTH_SUPPORT)
public class ClientCredentialsConnectionProviderWrapperTestCase {

  @Test
  public void listenerIsUnRegisteredOnStop() throws MuleException {
    ConnectionProvider delegate = mock(ConnectionProvider.class);
    ClientCredentialsConfig oauthConfig = mock(ClientCredentialsConfig.class);
    ClientCredentialsGrantType type = new ClientCredentialsGrantType("http://accessToken",
                                                                     "#[accessToken]",
                                                                     ".*",
                                                                     null,
                                                                     BODY);
    when(oauthConfig.getGrantType()).thenReturn(type);
    Map<Field, String> callbackValues = new HashMap<>();
    ClientCredentialsOAuthHandler oauthHandler = mock(ClientCredentialsOAuthHandler.class);
    when(oauthHandler.getOAuthContext(any())).thenReturn(mock(ResourceOwnerOAuthContext.class));
    ReconnectionConfig reconnectionConfig = mock(ReconnectionConfig.class);
    ClientCredentialsConnectionProviderWrapper wrapper =
        new TestClientCredentialsConnectionProviderWrapper(delegate, oauthConfig, callbackValues, oauthHandler,
                                                           reconnectionConfig);
    ClientCredentialsOAuthDancer dancer = mock(ClientCredentialsOAuthDancer.class);
    when(oauthHandler.register(any())).thenReturn(dancer);

    wrapper.start();
    wrapper.connect();
    verify(dancer, times(1)).addListener(any());
    wrapper.stop();
    verify(dancer, times(1)).removeListener(any());
  }

  private class TestClientCredentialsConnectionProviderWrapper extends ClientCredentialsConnectionProviderWrapper {

    public TestClientCredentialsConnectionProviderWrapper(ConnectionProvider delegate, ClientCredentialsConfig oauthConfig,
                                                          Map<Field, String> callbackValues,
                                                          ClientCredentialsOAuthHandler oauthHandler,
                                                          ReconnectionConfig reconnectionConfig) {
      super(delegate, oauthConfig, callbackValues, oauthHandler, reconnectionConfig);
    }

    @Override
    protected FieldSetter<Object, Object> resolveOauthStateSetter(ClientCredentialsConfig oauthConfig) {
      return mock(FieldSetter.class);
    }
  }
}
