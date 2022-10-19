/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.QUERY_PARAMS;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.ClientCredentials;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.property.oauth.OAuthCallbackValuesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ConnectionProviderTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.connection.JavaConnectionProviderModelParserUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JavaConnectionProviderModelParserTestCase {

  private JavaConnectionProviderModelParser parser;
  private ConnectionProviderElement connectionProviderElement;

  private static final String ACCESS_TOKEN_URL = "accessTokenUrl";
  private static final String AUTHORIZATION_URL = "authorizationUrl";
  private static final String ACCESS_TOKEN_EXPR = "accessTokenExpr";
  private static final String EXPIRATION_EXPR = "expirationExpr";
  private static final String REFRESH_TOKEN_EXPR = "refreshTokenExpr";
  private static final String DEFAULT_SCOPES = "defaultScopes";
  private static final CredentialsPlacement CREDENTIALS_PLACEMENT = QUERY_PARAMS;
  private static final org.mule.sdk.api.security.CredentialsPlacement SDK_CREDENTIALS_PLACEMENT =
      org.mule.sdk.api.security.CredentialsPlacement.QUERY_PARAMS;
  private static final boolean INCLUDE_REDIRECT_URI_IN_REFRESH_TOKEN_REQUEST = false;
  private static final String TOKEN_URL = "tokenUrl";
  private static final ValidationOAuthGrantTypeVisitor VALIDATION_O_AUTH_GRANT_TYPE_VISITOR =
      new ValidationOAuthGrantTypeVisitor();

  private static final String CALLBACK_FIELD_NAME = "callbackValue";
  private static final String SDK_CALLBACK_FIELD_NAME = "sdkCallbackValue";

  private static final String CALLBACK_EXPRESSION = "#[payload.callback]";
  private static final String SDK_CALLBACK_EXPRESSION = "#[payload.sdkCallback]";

  private static final Map<String, String> CALLBACK_BINDING = new HashMap<String, String>() {

    {
      put(CALLBACK_EXPRESSION, CALLBACK_FIELD_NAME);
      put(SDK_CALLBACK_EXPRESSION, SDK_CALLBACK_FIELD_NAME);
    }
  };

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void noOAuthSupport() {
    Optional<OAuthModelProperty> oAuthModelProperty =
        parseOAuthModelPropertyFromConnectionProviderClass(BaseTestConnectionProvider.class);
    assertThat(oAuthModelProperty.isPresent(), is(false));
  }

  @Test
  public void authorizationCodeSupport() {
    Optional<OAuthModelProperty> oAuthModelProperty =
        parseOAuthModelPropertyFromConnectionProviderClass(AuthorizationCodeConnectionProvider.class);
    assertThat(oAuthModelProperty.isPresent(), is(true));
    assertThat(oAuthModelProperty.get().getGrantTypes(), hasSize(1));
    assertThat(oAuthModelProperty.get().getGrantTypes().get(0), instanceOf(AuthorizationCodeGrantType.class));
    oAuthModelProperty.get().getGrantTypes().get(0).accept(VALIDATION_O_AUTH_GRANT_TYPE_VISITOR);
  }

  @Test
  public void sdkAuthorizationCodeSupport() {
    Optional<OAuthModelProperty> oAuthModelProperty =
        parseOAuthModelPropertyFromConnectionProviderClass(SdkAuthorizationCodeConnectionProvider.class);
    assertThat(oAuthModelProperty.isPresent(), is(true));
    assertThat(oAuthModelProperty.get().getGrantTypes(), hasSize(1));
    assertThat(oAuthModelProperty.get().getGrantTypes().get(0), instanceOf(AuthorizationCodeGrantType.class));
    oAuthModelProperty.get().getGrantTypes().get(0).accept(VALIDATION_O_AUTH_GRANT_TYPE_VISITOR);
  }

  @Test
  public void bothAuthorizationCodeSupport() {
    expectedException.expect(IllegalParameterModelDefinitionException.class);
    parseOAuthModelPropertyFromConnectionProviderClass(BothAuthorizationCodeConnectionProvider.class);
  }

  @Test
  public void clientCredentialsSupport() {
    Optional<OAuthModelProperty> oAuthModelProperty =
        parseOAuthModelPropertyFromConnectionProviderClass(ClientCredentialsConnectionProvider.class);
    assertThat(oAuthModelProperty.isPresent(), is(true));
    assertThat(oAuthModelProperty.get().getGrantTypes(), hasSize(1));
    assertThat(oAuthModelProperty.get().getGrantTypes().get(0), instanceOf(ClientCredentialsGrantType.class));
    oAuthModelProperty.get().getGrantTypes().get(0).accept(VALIDATION_O_AUTH_GRANT_TYPE_VISITOR);
  }

  @Test
  public void sdkClientCredentialsSupport() {
    Optional<OAuthModelProperty> oAuthModelProperty =
        parseOAuthModelPropertyFromConnectionProviderClass(SdkClientCredentialsConnectionProvider.class);
    assertThat(oAuthModelProperty.isPresent(), is(true));
    assertThat(oAuthModelProperty.get().getGrantTypes(), hasSize(1));
    assertThat(oAuthModelProperty.get().getGrantTypes().get(0), instanceOf(ClientCredentialsGrantType.class));
    oAuthModelProperty.get().getGrantTypes().get(0).accept(VALIDATION_O_AUTH_GRANT_TYPE_VISITOR);
  }

  @Test
  public void bothClientCredentialsSupport() {
    expectedException.expect(IllegalParameterModelDefinitionException.class);
    parseOAuthModelPropertyFromConnectionProviderClass(BothClientCredentialsConnectionProvider.class);
  }

  @Test
  public void oauthCallbackValues() {
    Optional<OAuthCallbackValuesModelProperty> oAuthCallbackValuesModelProperty =
        parseOAuthCallbackValuesModelPropertyFromConnectionProviderClass(OAuthCallbackValuesConnectionProvider.class);
    assertThat(oAuthCallbackValuesModelProperty.isPresent(), is(true));
    Map<Field, String> callbackValues = oAuthCallbackValuesModelProperty.get().getCallbackValues();
    assertThat(callbackValues.size(), is(2));
    callbackValues.entrySet().stream()
        .forEach(fieldStringEntry -> assertThat(fieldStringEntry.getKey().getName(),
                                                is(CALLBACK_BINDING.get(fieldStringEntry.getValue()))));
  }

  @Test
  public void noOauthCallbackValues() {
    Optional<OAuthCallbackValuesModelProperty> oAuthCallbackValuesModelProperty =
        parseOAuthCallbackValuesModelPropertyFromConnectionProviderClass(AuthorizationCodeConnectionProvider.class);
    assertThat(oAuthCallbackValuesModelProperty.isPresent(), is(false));
  }

  @Test
  public void noManagementStrategyConnectionProvider() {
    mockConnectionProviderWithClass(AbstractTransactionalConnectionProvider.class);
    assertThat(parser.getConnectionManagementType(), is(NONE));
  }

  @Test
  public void noManagementStrategySdkConnectionProvider() {
    mockConnectionProviderWithClass(SdkAbstractTransactionalConnectionProvider.class);
    assertThat(parser.getConnectionManagementType(), is(NONE));
  }

  @Test
  public void isPoolingConnectionProvider() {
    mockConnectionProviderWithClass(PoolingTransactionalConnectionProvider.class);
    assertThat(parser.getConnectionManagementType(), is(POOLING));
  }

  @Test
  public void isSdkPoolingConnectionProvider() {
    mockConnectionProviderWithClass(SdkPoolingTransactionalConnectionProvider.class);
    assertThat(parser.getConnectionManagementType(), is(POOLING));
  }

  @Test
  public void isCachedConnectionProvider() {
    mockConnectionProviderWithClass(CachedTransactionalConnectionProvider.class);
    assertThat(parser.getConnectionManagementType(), is(CACHED));
  }

  @Test
  public void isSdkCachedConnectionProvider() {
    mockConnectionProviderWithClass(SdkCachedTransactionalConnectionProvider.class);
    assertThat(parser.getConnectionManagementType(), is(CACHED));
  }

  @Test
  public void getMinMuleVersionConnectionProvider() {
    mockConnectionProviderWithClass(BaseTestConnectionProvider.class);
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    Assert.assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    Assert.assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.1.0"));
  }

  @Test
  public void getMinMuleVersionSdkConnectionProvider() {
    mockConnectionProviderWithClass(SdkCachedTransactionalConnectionProvider.class);
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    Assert.assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    Assert.assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.5.0"));
  }

  private static class ValidationOAuthGrantTypeVisitor implements OAuthGrantTypeVisitor {

    @Override
    public void visit(AuthorizationCodeGrantType grantType) {
      assertThat(grantType.getAccessTokenExpr(), is(ACCESS_TOKEN_EXPR));
      assertThat(grantType.getAccessTokenUrl(), is(ACCESS_TOKEN_URL));
      assertThat(grantType.getAuthorizationUrl(), is(AUTHORIZATION_URL));
      assertThat(grantType.getCredentialsPlacement(), is(CREDENTIALS_PLACEMENT));
      assertThat(grantType.getDefaultScope().get(), is(DEFAULT_SCOPES));
      assertThat(grantType.getExpirationRegex(), is(EXPIRATION_EXPR));
      assertThat(grantType.getRefreshTokenExpr(), is(REFRESH_TOKEN_EXPR));
      assertThat(grantType.includeRedirectUriInRefreshTokenRequest(), is(INCLUDE_REDIRECT_URI_IN_REFRESH_TOKEN_REQUEST));
    }

    @Override
    public void visit(ClientCredentialsGrantType grantType) {
      assertThat(grantType.getAccessTokenExpr(), is(ACCESS_TOKEN_EXPR));
      assertThat(grantType.getCredentialsPlacement(), is(CREDENTIALS_PLACEMENT));
      assertThat(grantType.getDefaultScopes().get(), is(DEFAULT_SCOPES));
      assertThat(grantType.getExpirationRegex(), is(EXPIRATION_EXPR));
      assertThat(grantType.getTokenUrl(), is(TOKEN_URL));
    }

    @Override
    public void visit(PlatformManagedOAuthGrantType grantType) {

    }
  }

  private Optional<OAuthModelProperty> parseOAuthModelPropertyFromConnectionProviderClass(Class<?> connectionProviderClass) {
    mockConnectionProviderWithClass(connectionProviderClass);
    return parser.getOAuthModelProperty();
  }

  private Optional<OAuthCallbackValuesModelProperty> parseOAuthCallbackValuesModelPropertyFromConnectionProviderClass(Class<?> connectionProviderClass) {
    mockConnectionProviderWithClass(connectionProviderClass);
    return parser.getAdditionalModelProperties().stream()
        .filter(modelProperty -> modelProperty instanceof OAuthCallbackValuesModelProperty)
        .map(OAuthCallbackValuesModelProperty.class::cast).findFirst();
  }

  private void mockConnectionProviderWithClass(Class<?> connectionProviderClass) {
    connectionProviderElement =
        new ConnectionProviderTypeWrapper(connectionProviderClass, new DefaultExtensionsTypeLoaderFactory()
            .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    parser = new JavaConnectionProviderModelParser(mock(JavaExtensionModelParser.class), mock(ExtensionElement.class),
                                                   connectionProviderElement);
  }

  private static class BaseTestConnectionProvider implements ConnectionProvider {

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



  @AuthorizationCode(accessTokenUrl = ACCESS_TOKEN_URL, authorizationUrl = AUTHORIZATION_URL, accessTokenExpr = ACCESS_TOKEN_EXPR,
      expirationExpr = EXPIRATION_EXPR, refreshTokenExpr = REFRESH_TOKEN_EXPR, defaultScopes = DEFAULT_SCOPES,
      credentialsPlacement = QUERY_PARAMS,
      includeRedirectUriInRefreshTokenRequest = INCLUDE_REDIRECT_URI_IN_REFRESH_TOKEN_REQUEST)
  private static class AuthorizationCodeConnectionProvider extends BaseTestConnectionProvider {

  }

  @org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode(accessTokenUrl = ACCESS_TOKEN_URL,
      authorizationUrl = AUTHORIZATION_URL, accessTokenExpr = ACCESS_TOKEN_EXPR, expirationExpr = EXPIRATION_EXPR,
      refreshTokenExpr = REFRESH_TOKEN_EXPR, defaultScopes = DEFAULT_SCOPES,
      credentialsPlacement = org.mule.sdk.api.security.CredentialsPlacement.QUERY_PARAMS,
      includeRedirectUriInRefreshTokenRequest = INCLUDE_REDIRECT_URI_IN_REFRESH_TOKEN_REQUEST)
  private static class SdkAuthorizationCodeConnectionProvider extends BaseTestConnectionProvider {

  }

  @AuthorizationCode(accessTokenUrl = ACCESS_TOKEN_URL, authorizationUrl = AUTHORIZATION_URL, accessTokenExpr = ACCESS_TOKEN_EXPR,
      expirationExpr = EXPIRATION_EXPR, refreshTokenExpr = REFRESH_TOKEN_EXPR, defaultScopes = DEFAULT_SCOPES,
      credentialsPlacement = QUERY_PARAMS,
      includeRedirectUriInRefreshTokenRequest = INCLUDE_REDIRECT_URI_IN_REFRESH_TOKEN_REQUEST)
  @org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode(accessTokenUrl = ACCESS_TOKEN_URL,
      authorizationUrl = AUTHORIZATION_URL, accessTokenExpr = ACCESS_TOKEN_EXPR, expirationExpr = EXPIRATION_EXPR,
      refreshTokenExpr = REFRESH_TOKEN_EXPR, defaultScopes = DEFAULT_SCOPES,
      credentialsPlacement = org.mule.sdk.api.security.CredentialsPlacement.QUERY_PARAMS,
      includeRedirectUriInRefreshTokenRequest = INCLUDE_REDIRECT_URI_IN_REFRESH_TOKEN_REQUEST)
  private static class BothAuthorizationCodeConnectionProvider extends BaseTestConnectionProvider {

  }

  @ClientCredentials(tokenUrl = TOKEN_URL, accessTokenExpr = ACCESS_TOKEN_EXPR, expirationExpr = EXPIRATION_EXPR,
      defaultScopes = DEFAULT_SCOPES, credentialsPlacement = QUERY_PARAMS)
  private static class ClientCredentialsConnectionProvider extends BaseTestConnectionProvider {

  }

  @org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials(tokenUrl = TOKEN_URL, accessTokenExpr = ACCESS_TOKEN_EXPR,
      expirationExpr = EXPIRATION_EXPR, defaultScopes = DEFAULT_SCOPES,
      credentialsPlacement = org.mule.sdk.api.security.CredentialsPlacement.QUERY_PARAMS)
  private static class SdkClientCredentialsConnectionProvider extends BaseTestConnectionProvider {

  }

  @ClientCredentials(tokenUrl = TOKEN_URL, accessTokenExpr = ACCESS_TOKEN_EXPR, expirationExpr = EXPIRATION_EXPR,
      defaultScopes = DEFAULT_SCOPES, credentialsPlacement = QUERY_PARAMS)
  @org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials(tokenUrl = TOKEN_URL, accessTokenExpr = ACCESS_TOKEN_EXPR,
      expirationExpr = EXPIRATION_EXPR, defaultScopes = DEFAULT_SCOPES,
      credentialsPlacement = org.mule.sdk.api.security.CredentialsPlacement.QUERY_PARAMS)
  private static class BothClientCredentialsConnectionProvider extends BaseTestConnectionProvider {

  }


  @AuthorizationCode(accessTokenUrl = ACCESS_TOKEN_URL, authorizationUrl = AUTHORIZATION_URL, accessTokenExpr = ACCESS_TOKEN_EXPR,
      expirationExpr = EXPIRATION_EXPR, refreshTokenExpr = REFRESH_TOKEN_EXPR, defaultScopes = DEFAULT_SCOPES,
      credentialsPlacement = QUERY_PARAMS,
      includeRedirectUriInRefreshTokenRequest = INCLUDE_REDIRECT_URI_IN_REFRESH_TOKEN_REQUEST)
  private static class OAuthCallbackValuesConnectionProvider extends BaseTestConnectionProvider {

    @OAuthCallbackValue(expression = CALLBACK_EXPRESSION)
    private String callbackValue;

    @org.mule.sdk.api.annotation.connectivity.oauth.OAuthCallbackValue(expression = SDK_CALLBACK_EXPRESSION)
    private String sdkCallbackValue;

    @Parameter
    private String nonCallbackValue;


  }

  interface TestTransactionalConnection extends TransactionalConnection {
  }

  private class AbstractTransactionalConnectionProvider implements ConnectionProvider<TestTransactionalConnection> {

    @Override
    public TestTransactionalConnection connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(TestTransactionalConnection connection) {

    }

    @Override
    public ConnectionValidationResult validate(TestTransactionalConnection connection) {
      return null;
    }
  }

  private class PoolingTransactionalConnectionProvider extends AbstractTransactionalConnectionProvider
      implements PoolingConnectionProvider<TestTransactionalConnection> {
  }

  private class CachedTransactionalConnectionProvider extends AbstractTransactionalConnectionProvider
      implements CachedConnectionProvider<TestTransactionalConnection> {
  }

  protected interface SdkTestTransactionalConnection extends org.mule.sdk.api.connectivity.TransactionalConnection {
  }

  class SdkAbstractTransactionalConnectionProvider
      implements org.mule.sdk.api.connectivity.ConnectionProvider<SdkTestTransactionalConnection> {

    @Override
    public SdkTestTransactionalConnection connect() throws ConnectionException {
      return null;
    }

    @Override
    public void disconnect(SdkTestTransactionalConnection connection) {

    }

    @Override
    public org.mule.sdk.api.connectivity.ConnectionValidationResult validate(SdkTestTransactionalConnection connection) {
      return null;
    }
  }

  private class SdkPoolingTransactionalConnectionProvider extends SdkAbstractTransactionalConnectionProvider
      implements org.mule.sdk.api.connectivity.PoolingConnectionProvider<SdkTestTransactionalConnection> {
  }

  private class SdkCachedTransactionalConnectionProvider extends SdkAbstractTransactionalConnectionProvider
      implements org.mule.sdk.api.connectivity.CachedConnectionProvider<SdkTestTransactionalConnection> {
  }
}
