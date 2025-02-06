/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class BaseOAuthConnectionProviderWrapperTestCase {

  private BaseOAuthConnectionProviderWrapper<Object> providerWrapper;
  private ConnectionProvider<Object> delegate;
  private ReconnectionConfig reconnectionConfig;
  private Map<Field, String> callbackValues;
  private ResourceOwnerOAuthContext context;

  @Before
  public void setUp() {
    delegate = mock(ConnectionProvider.class);
    reconnectionConfig = mock(ReconnectionConfig.class);
    callbackValues = new HashMap<>();
    context = mock(ResourceOwnerOAuthContext.class);

    providerWrapper = new BaseOAuthConnectionProviderWrapper<>(delegate, reconnectionConfig, callbackValues) {

      @Override
      public Optional<String> getOwnerConfigName() {
        return super.getOwnerConfigName();
      }

      @Override
      public OAuthGrantType getGrantType() {
        return null;
      }

      @Override
      public void refreshToken(String resourceOwnerId) {

      }

      @Override
      public void invalidate(String resourceOwnerId) {

      }

      @Override
      protected ResourceOwnerOAuthContext getContext() {
        return context;
      }
    };
  }

  @Test
  public void testValidate() {
    Object connection = new Object();
    ConnectionValidationResult expectedResult = mock(ConnectionValidationResult.class);
    when(delegate.validate(connection)).thenReturn(expectedResult);
    when(context.getAccessToken()).thenReturn("validAccessToken");

    ConnectionValidationResult result = providerWrapper.validate(connection);

    assertThat(result, is(expectedResult));
    verify(delegate).validate(connection);
  }

  @Test
  public void testGetResourceOwnerId() {
    String resourceOwnerId = "resourceOwnerId123";
    when(context.getResourceOwnerId()).thenReturn(resourceOwnerId);

    String result = providerWrapper.getResourceOwnerId();

    assertThat(result, is(resourceOwnerId));
  }
}
