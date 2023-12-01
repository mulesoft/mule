/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.mule.runtime.extension.api.security.CredentialsPlacement.BODY;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ClientCredentialsConnectionProviderWrapperTestCase {

  @Test
  public void getUpdatingClientCredentialsState() throws MuleException {
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
    ReconnectionConfig reconnectionConfig = mock(ReconnectionConfig.class);
    ClientCredentialsConnectionProviderWrapper wrapper =
        new ClientCredentialsConnectionProviderWrapper(delegate, oauthConfig, callbackValues, oauthHandler, reconnectionConfig);
    ClientCredentialsOAuthDancer dancer = mock(ClientCredentialsOAuthDancer.class);
    when(oauthHandler.register(any())).thenReturn(dancer);

    assertThat(wrapper.getUpdatingClientCredentialsState(), is(nullValue()));
    wrapper.start();
    wrapper.connect();
    assertThat(wrapper.getUpdatingClientCredentialsState(), isNotNull());
    verify(dancer, times(1)).addListener(any());
    wrapper.stop();
    verify(dancer, times(1)).removeListener(any());
  }
}
