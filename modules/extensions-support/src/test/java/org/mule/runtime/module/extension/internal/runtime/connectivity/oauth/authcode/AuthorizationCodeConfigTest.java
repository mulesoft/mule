/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.CustomOAuthParameters;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthObjectStoreConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class AuthorizationCodeConfigTest {

  private static final String OWNER_CONFIG_NAME = "ownerConfig";
  private static final String CONSUMER_KEY = "consumerKey";
  private static final String CONSUMER_SECRET = "consumerSecret";
  private static final String AUTH_URL = "https://auth.url";
  private static final String TOKEN_URL = "https://token.url";
  private static final String SCOPE = "read write";
  private static final String BEFORE = "before";
  private static final String AFTER = "after";
  private static final String RESOURCE_OWNER_ID = "resourceOwnerId";

  private AuthorizationCodeGrantType grantType;
  private Optional<OAuthObjectStoreConfig> storeConfig;
  private CustomOAuthParameters customOAuthParameters;
  private Map<Field, String> parameterExtractors;
  private OAuthCallbackConfig callbackConfig;

  private AuthorizationCodeConfig config;

  @Before
  public void setUp() {
    grantType = mock(AuthorizationCodeGrantType.class);
    storeConfig = Optional.of(mock(OAuthObjectStoreConfig.class));
    customOAuthParameters = mock(CustomOAuthParameters.class);
    parameterExtractors = mock(Map.class);
    callbackConfig = mock(OAuthCallbackConfig.class);

    config = new AuthorizationCodeConfig(
                                         OWNER_CONFIG_NAME,
                                         storeConfig,
                                         customOAuthParameters,
                                         parameterExtractors,
                                         grantType,
                                         callbackConfig,
                                         CONSUMER_KEY,
                                         CONSUMER_SECRET,
                                         AUTH_URL,
                                         TOKEN_URL,
                                         SCOPE,
                                         RESOURCE_OWNER_ID,
                                         BEFORE,
                                         AFTER);
  }

  @Test
  public void testGetConsumerKey() {
    assertThat(config.getConsumerKey(), is(CONSUMER_KEY));
  }

  @Test
  public void testGetConsumerSecret() {
    assertThat(config.getConsumerSecret(), is(CONSUMER_SECRET));
  }

  @Test
  public void testGetAccessTokenUrl() {
    assertThat(config.getAccessTokenUrl(), is(TOKEN_URL));
  }

  @Test
  public void testGetAuthorizationUrl() {
    assertThat(config.getAuthorizationUrl(), is(AUTH_URL));
  }

  @Test
  public void testGetScope() {
    assertThat(config.getScope().isPresent(), is(true));
    assertThat(config.getScope().get(), is(SCOPE));
  }

  @Test
  public void testGetScopeWhenNull() {
    AuthorizationCodeConfig configWithNullScope = new AuthorizationCodeConfig(
                                                                              OWNER_CONFIG_NAME,
                                                                              storeConfig,
                                                                              customOAuthParameters,
                                                                              parameterExtractors,
                                                                              grantType,
                                                                              callbackConfig,
                                                                              CONSUMER_KEY,
                                                                              CONSUMER_SECRET,
                                                                              AUTH_URL,
                                                                              TOKEN_URL,
                                                                              null,
                                                                              RESOURCE_OWNER_ID,
                                                                              BEFORE,
                                                                              AFTER);
    assertThat(configWithNullScope.getScope().isPresent(), is(false));
  }

  @Test
  public void testGetGrantType() {
    assertThat(config.getGrantType(), is(grantType));
  }

  @Test
  public void testGetResourceOwnerId() {
    assertThat(config.getResourceOwnerId(), is(RESOURCE_OWNER_ID));
  }

  @Test
  public void testGetCallbackConfig() {
    assertThat(config.getCallbackConfig(), is(callbackConfig));
  }

  @Test
  public void testGetBefore() {
    assertThat(config.getBefore().isPresent(), is(true));
    assertThat(config.getBefore().get(), is(BEFORE));
  }

  @Test
  public void testGetAfter() {
    assertThat(config.getAfter().isPresent(), is(true));
    assertThat(config.getAfter().get(), is(AFTER));
  }
}
