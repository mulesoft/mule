/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.builder.ClientCredentialsLocation;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.OAuthCallbackConfig;

import org.junit.Test;

public class ExtensionsOAuthUtilsTestCase {

  @Test
  public void testToAuthorizationCodeState() {
    AuthorizationCodeConfig mockAuthCodeConfig = mock(AuthorizationCodeConfig.class);
    ResourceOwnerOAuthContext mockContext = mock(ResourceOwnerOAuthContext.class);

    when(mockAuthCodeConfig.getCallbackConfig()).thenReturn(mock(OAuthCallbackConfig.class));

    AuthorizationCodeState state = ExtensionsOAuthUtils.toAuthorizationCodeState(mockAuthCodeConfig, mockContext);
    assertThat(state, is(notNullValue()));
  }

  @Test
  public void testToCredentialsLocation() {
    assertThat(ExtensionsOAuthUtils.toCredentialsLocation(CredentialsPlacement.BASIC_AUTH_HEADER),
               is(ClientCredentialsLocation.BASIC_AUTH_HEADER));
    assertThat(ExtensionsOAuthUtils.toCredentialsLocation(CredentialsPlacement.QUERY_PARAMS),
               is(ClientCredentialsLocation.QUERY_PARAMS));
    assertThat(ExtensionsOAuthUtils.toCredentialsLocation(CredentialsPlacement.BODY), is(ClientCredentialsLocation.BODY));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToCredentialsLocationWhenInvalid() {
    CredentialsPlacement mockPlacement = mock(CredentialsPlacement.class);
    when(mockPlacement.name()).thenReturn("INVALID");
    ExtensionsOAuthUtils.toCredentialsLocation(mockPlacement);
  }

  @Test
  public void testWithRefreshToken() throws Throwable {
    ConnectionProvider mockConnectionProvider = mock(ConnectionProvider.class);

    CheckedSupplier<String> mockSupplier = mock(CheckedSupplier.class);
    when(mockSupplier.getChecked()).thenReturn("Success");

    String result = ExtensionsOAuthUtils.withRefreshToken(mockConnectionProvider, mockSupplier);

    assertThat(result, is("Success"));
    verify(mockSupplier, times(1)).getChecked();
  }

  @Test
  public void testWithRefreshTokenWhenExceptionThrown() throws Throwable {
    ConnectionProvider mockConnectionProvider = mock(ConnectionProvider.class);

    CheckedSupplier<String> mockSupplier = mock(CheckedSupplier.class);
    when(mockSupplier.getChecked()).thenThrow(new RuntimeException("Test Exception"));

    assertThrows(RuntimeException.class, () -> ExtensionsOAuthUtils.withRefreshToken(mockConnectionProvider, mockSupplier));

    verify(mockSupplier, times(1)).getChecked();
  }
}
