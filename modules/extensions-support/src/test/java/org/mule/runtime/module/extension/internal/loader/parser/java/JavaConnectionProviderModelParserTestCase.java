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
import static org.mule.runtime.extension.api.security.CredentialsPlacement.QUERY_PARAMS;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.ClientCredentials;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ConnectionProviderTypeWrapper;

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
    Optional<OAuthModelProperty> oAuthModelProperty =
        parseOAuthModelPropertyFromConnectionProviderClass(BothAuthorizationCodeConnectionProvider.class);
  }

  @Test
  public void clientCredentialsSupport() {}

  @Test
  public void sdkClientCredentialsSupport() {}

  @Test
  public void bothClientCredentialsSupport() {}

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

    }

    @Override
    public void visit(PlatformManagedOAuthGrantType grantType) {

    }
  }

  private Optional<OAuthModelProperty> parseOAuthModelPropertyFromConnectionProviderClass(Class<?> connectionProviderClass) {
    mockConnectionProviderWithClass(connectionProviderClass);
    return parser.getOAuthModelProperty();
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
}
