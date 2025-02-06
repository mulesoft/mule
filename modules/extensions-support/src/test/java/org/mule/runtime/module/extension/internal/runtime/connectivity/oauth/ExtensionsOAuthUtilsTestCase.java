/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.oauth.client.api.builder.ClientCredentialsLocation;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.OAuthCallbackConfig;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.sdk.api.connectivity.oauth.AccessTokenExpiredException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  @Test
  public void testRefreshTokenIfNecessary() {
    ExecutionContextAdapter<OperationModel> operationContext = mock(ExecutionContextAdapter.class);
    OAuthConnectionProviderWrapper connectionProvider = mock(OAuthConnectionProviderWrapper.class);
    Throwable exception = mock(AccessTokenExpiredException.class);

    mockExecutionContext(operationContext, connectionProvider);
    when(connectionProvider.getGrantType()).thenReturn(mock(AuthorizationCodeGrantType.class));

    boolean result = ExtensionsOAuthUtils.refreshTokenIfNecessary(operationContext, exception);
    assertThat(result, is(true));
    verify(connectionProvider, times(1)).refreshToken(any());
  }

  @Test
  public void testRefreshTokenIfNecessaryWhenAccessTokenNotExpired() {
    ExecutionContextAdapter<OperationModel> operationContext = mock(ExecutionContextAdapter.class);
    OAuthConnectionProviderWrapper connectionProvider = mock(OAuthConnectionProviderWrapper.class);
    Throwable exception = mock(RuntimeException.class);

    mockExecutionContext(operationContext, connectionProvider);

    boolean result = ExtensionsOAuthUtils.refreshTokenIfNecessary(operationContext, exception);
    assertThat(result, is(false));
  }

  @Test(expected = MuleRuntimeException.class)
  public void testRefreshTokenIfNecessaryWhenRefreshTokenFails() {
    ExecutionContextAdapter<OperationModel> operationContext = mock(ExecutionContextAdapter.class);
    OAuthConnectionProviderWrapper connectionProvider = mock(OAuthConnectionProviderWrapper.class);
    Throwable exception = mock(AccessTokenExpiredException.class);

    mockExecutionContext(operationContext, connectionProvider);
    when(connectionProvider.getGrantType()).thenReturn(mock(AuthorizationCodeGrantType.class));
    when(connectionProvider.getResourceOwnerId()).thenReturn("testUser");
    doThrow(new RuntimeException("Refresh failed")).when(connectionProvider).refreshToken(any());

    ExtensionsOAuthUtils.refreshTokenIfNecessary(operationContext, exception);
  }

  @Test
  public void testGetOAuthStateSetter() {
    TestClassSingleState target = new TestClassSingleState();
    OAuthGrantType mockGrantType = mock(OAuthGrantType.class);
    when(mockGrantType.getName()).thenReturn("MockGrant");

    FieldSetter<Object, Object> setter =
        ExtensionsOAuthUtils.getOAuthStateSetter(target, List.of(OAuthState.class), mockGrantType);
    assertThat(setter, is(notNullValue()));
  }

  @Test(expected = IllegalConnectionProviderModelDefinitionException.class)
  public void testGetOAuthStateSetterWhenNoMatchingField() {
    TestClassNoState target = new TestClassNoState();
    OAuthGrantType mockGrantType = mock(OAuthGrantType.class);
    when(mockGrantType.getName()).thenReturn("MockGrant");

    ExtensionsOAuthUtils.getOAuthStateSetter(target, List.of(OAuthState.class), mockGrantType);
  }

  @Test
  public void testUpdateOAuthParameters() throws NoSuchFieldException, IllegalAccessException {
    TestTarget target = new TestTarget();
    ResourceOwnerOAuthContext context = mock(ResourceOwnerOAuthContext.class);

    Map<String, Object> responseParameters = new HashMap<>();
    responseParameters.put("accessToken", "newAccessToken");
    responseParameters.put("refreshToken", "newRefreshToken");

    when(context.getTokenResponseParameters()).thenReturn(responseParameters);

    Field accessTokenField = TestTarget.class.getDeclaredField("accessToken");
    Field refreshTokenField = TestTarget.class.getDeclaredField("refreshToken");
    accessTokenField.setAccessible(true);
    refreshTokenField.setAccessible(true);

    Map<Field, String> callbackValues = new HashMap<>();
    callbackValues.put(accessTokenField, "accessToken");
    callbackValues.put(refreshTokenField, "refreshToken");

    ExtensionsOAuthUtils.updateOAuthParameters(target, callbackValues, context);

    assertThat(accessTokenField.get(target), is("newAccessToken"));
    assertThat(refreshTokenField.get(target), is("newRefreshToken"));
  }

  @Test
  public void testValidateOAuthConnection() {
    ConnectionProvider<Object> connectionProvider = mock(ConnectionProvider.class);
    Object connection = new Object();
    ResourceOwnerOAuthContext context = mock(ResourceOwnerOAuthContext.class);
    String accessToken = "validAccessToken";
    when(context.getAccessToken()).thenReturn(accessToken);
    ConnectionValidationResult expectedResult = mock(ConnectionValidationResult.class);
    when(connectionProvider.validate(connection)).thenReturn(expectedResult);

    ConnectionValidationResult result = ExtensionsOAuthUtils.validateOAuthConnection(connectionProvider, connection, context);

    assertThat(result, is(expectedResult));
    verify(connectionProvider).validate(connection);
  }

  @Test
  public void testValidateOAuthConnectionWhenNoAccessToken() {
    ConnectionProvider<Object> connectionProvider = mock(ConnectionProvider.class);
    Object connection = new Object();
    ResourceOwnerOAuthContext context = mock(ResourceOwnerOAuthContext.class);
    when(context.getAccessToken()).thenReturn(null);

    ConnectionValidationResult result = ExtensionsOAuthUtils.validateOAuthConnection(connectionProvider, connection, context);

    assertThat(result, is(notNullValue()));
    assertThat(result.isValid(), is(false));
    assertThat(result.getException(), isA(IllegalStateException.class));
  }

  @Test
  public void testValidateOAuthConnectionWhenExceptionDuringValidation() {
    ConnectionProvider<Object> connectionProvider = mock(ConnectionProvider.class);
    Object connection = new Object();
    ResourceOwnerOAuthContext context = mock(ResourceOwnerOAuthContext.class);
    String accessToken = "validAccessToken";
    when(context.getAccessToken()).thenReturn(accessToken);
    when(connectionProvider.validate(connection)).thenThrow(new RuntimeException("Validation failed"));

    ConnectionValidationResult result = ExtensionsOAuthUtils.validateOAuthConnection(connectionProvider, connection, context);

    assertThat(result, is(notNullValue()));
    assertThat(result.isValid(), is(false));
    assertThat(result.getException(), isA(RuntimeException.class));
  }

  private void mockExecutionContext(ExecutionContextAdapter<OperationModel> operationContext,
                                    OAuthConnectionProviderWrapper provider) {
    OperationModel operationModel = mock(OperationModel.class);
    ExtensionModel extensionModel = mock(ExtensionModel.class);
    ConfigurationInstance configInstance = mock(ConfigurationInstance.class);
    ConnectionProvider connectionProvider = provider;

    when(operationContext.getExtensionModel()).thenReturn(extensionModel);
    when(operationContext.getComponentModel()).thenReturn(operationModel);
    when(operationContext.getConfiguration()).thenReturn(Optional.of(configInstance));
    when(configInstance.getConnectionProvider()).thenReturn(Optional.ofNullable(connectionProvider));
  }

  public static class TestTarget {

    private String accessToken;
    private String refreshToken;

    public String getAccessToken() {
      return accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }
  }

  public static class TestClassSingleState {

    private OAuthState state;
  }

  public static class TestClassNoState {

    private String someField;
  }
}
