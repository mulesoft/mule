/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.CustomOAuthParameters;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthObjectStoreConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public class ClientCredentialsConfigTest {

  private static final String OWNER_CONFIG_NAME = "ownerConfig";
  private static final String CLIENT_ID = "testClientId";
  private static final String CLIENT_SECRET = "testClientSecret";
  private static final String TOKEN_URL = "https://token.url";
  private static final String SCOPE = "read write";
  private static final CredentialsPlacement CREDENTIALS_PLACEMENT = CredentialsPlacement.BASIC_AUTH_HEADER;

  private ClientCredentialsGrantType grantType;
  private Optional<OAuthObjectStoreConfig> storeConfig;
  private CustomOAuthParameters customOAuthParameters;
  private Map<Field, String> parameterExtractors;

  private ClientCredentialsConfig config;

  @Before
  public void setUp() {
    grantType = mock(ClientCredentialsGrantType.class);
    storeConfig = Optional.of(mock(OAuthObjectStoreConfig.class));
    customOAuthParameters = mock(CustomOAuthParameters.class);
    parameterExtractors = mock(Map.class);

    config = new ClientCredentialsConfig(
                                         OWNER_CONFIG_NAME,
                                         storeConfig,
                                         customOAuthParameters,
                                         parameterExtractors,
                                         CLIENT_ID,
                                         CLIENT_SECRET,
                                         TOKEN_URL,
                                         SCOPE,
                                         CREDENTIALS_PLACEMENT,
                                         grantType);
  }

  @Test
  public void testGetClientId() {
    assertThat(config.getClientId(), is(CLIENT_ID));
  }

  @Test
  public void testGetClientSecret() {
    assertThat(config.getClientSecret(), is(CLIENT_SECRET));
  }

  @Test
  public void testGetTokenUrl() {
    assertThat(config.getTokenUrl(), is(TOKEN_URL));
  }

  @Test
  public void testGetCredentialsPlacement() {
    assertThat(config.getCredentialsPlacement(), is(CREDENTIALS_PLACEMENT));
  }

  @Test
  public void testGetScope() {
    assertThat(config.getScope().isPresent(), is(true));
    assertThat(config.getScope().get(), is(SCOPE));
  }

  @Test
  public void testGetScopeWhenNull() {
    ClientCredentialsConfig configWithNullScope = new ClientCredentialsConfig(
                                                                              OWNER_CONFIG_NAME,
                                                                              storeConfig,
                                                                              customOAuthParameters,
                                                                              parameterExtractors,
                                                                              CLIENT_ID,
                                                                              CLIENT_SECRET,
                                                                              TOKEN_URL,
                                                                              null,
                                                                              CREDENTIALS_PLACEMENT,
                                                                              grantType);
    assertThat(configWithNullScope.getScope().isPresent(), is(false));
  }

  @Test
  public void testGetConfigIdentifier() {
    String expectedIdentifier = OWNER_CONFIG_NAME + "//" + CLIENT_ID + "//" + CLIENT_SECRET + "//" + TOKEN_URL + "//" + SCOPE;
    assertThat(config.getConfigIdentifier(), is(expectedIdentifier));
  }

  @Test
  public void testGetGrantType() {
    assertThat(config.getGrantType(), is(grantType));
  }
}
