/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BASIC_AUTH_HEADER;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BODY;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.findConnectionProvider;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.tck.size.SmallTest;
import org.mule.test.oauth.TestOAuthExtension;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class OAuthConnectionProviderModelLoaderTestCase extends AbstractJavaExtensionDeclarationTestCase {

  private ExtensionDeclaration extensionDeclaration;

  @Before
  public void setUp() {
    setDeclarer(declarerFor(TestOAuthExtension.class));
    extensionDeclaration = declareExtension().getDeclaration();
  }

  @Test
  public void credentialsPlacementSettingForAuthorizationCodeIsLoaded() {
    ConfigurationDeclaration configuration = getConfiguration(extensionDeclaration, "auth-code");
    ConnectionProviderDeclaration connectionProviderDeclaration =
        findConnectionProvider(configuration, "with-credentials-placement");
    AuthorizationCodeGrantType authorizationCodeGrantType = getAuthorizationCodeGrantType(connectionProviderDeclaration);
    assertThat(authorizationCodeGrantType.getCredentialsPlacement(), is(BASIC_AUTH_HEADER));
  }

  @Test
  public void credentialsPlacementDefaultForAuthorizationCodeIsLoaded() {
    ConfigurationDeclaration configuration = getConfiguration(extensionDeclaration, "auth-code");
    ConnectionProviderDeclaration connectionProviderDeclaration =
        findConnectionProvider(configuration, "scopeless");
    AuthorizationCodeGrantType authorizationCodeGrantType = getAuthorizationCodeGrantType(connectionProviderDeclaration);
    assertThat(authorizationCodeGrantType.getCredentialsPlacement(), is(BODY));
  }

  @Test
  public void includeRedirectUriInRefreshTokenRequestSettingForAuthorizationCodeIsLoaded() {
    ConfigurationDeclaration configuration = getConfiguration(extensionDeclaration, "auth-code");
    ConnectionProviderDeclaration connectionProviderDeclaration =
        findConnectionProvider(configuration, "do-not-include-redirect-uri-in-refresh-token");
    AuthorizationCodeGrantType authorizationCodeGrantType = getAuthorizationCodeGrantType(connectionProviderDeclaration);
    assertThat(authorizationCodeGrantType.includeRedirectUriInRefreshTokenRequest(), is(false));
  }

  @Test
  public void includeRedirectUriInRefreshTokenRequestDefaultForAuthorizationCodeIsLoaded() {
    ConfigurationDeclaration configuration = getConfiguration(extensionDeclaration, "auth-code");
    ConnectionProviderDeclaration connectionProviderDeclaration =
        findConnectionProvider(configuration, "scopeless");
    AuthorizationCodeGrantType authorizationCodeGrantType = getAuthorizationCodeGrantType(connectionProviderDeclaration);
    assertThat(authorizationCodeGrantType.includeRedirectUriInRefreshTokenRequest(), is(true));
  }

  private AuthorizationCodeGrantType getAuthorizationCodeGrantType(ConnectionProviderDeclaration connectionProviderDeclaration) {
    Optional<OAuthModelProperty> oAuthModelProperty = connectionProviderDeclaration.getModelProperty(OAuthModelProperty.class);
    assertThat(oAuthModelProperty.isPresent(), is(true));
    List<OAuthGrantType> grantTypes = oAuthModelProperty.get().getGrantTypes();
    assertThat(grantTypes.size(), is(1));
    assertThat(grantTypes.get(0), instanceOf(AuthorizationCodeGrantType.class));
    return (AuthorizationCodeGrantType) grantTypes.get(0);
  }
}
