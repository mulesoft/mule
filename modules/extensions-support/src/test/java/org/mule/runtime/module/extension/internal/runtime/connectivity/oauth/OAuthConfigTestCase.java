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
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class OAuthConfigTestCase {

  private TestOAuthConfig testOAuthConfig;

  @Test
  public void testGetQueryParams() {
    Optional<OAuthObjectStoreConfig> mockStoreConfig = Optional.of(mock(OAuthObjectStoreConfig.class));
    CustomOAuthParameters parameters = new CustomOAuthParameters();
    Map<Field, String> parameterExtractors = new HashMap<>();

    testOAuthConfig = new TestOAuthConfig("", mockStoreConfig, parameters, parameterExtractors);
    MultiMap<String, String> map = testOAuthConfig.getCustomQueryParameters();

    assertThat(map, is(notNullValue()));
    assertThat(map.size(), is(0));
  }

  @Test
  public void testGetCustomBodyParameters() {
    Optional<OAuthObjectStoreConfig> mockStoreConfig = Optional.of(mock(OAuthObjectStoreConfig.class));
    CustomOAuthParameters parameters = new CustomOAuthParameters();
    Map<Field, String> parameterExtractors = new HashMap<>();

    testOAuthConfig = new TestOAuthConfig("", mockStoreConfig, parameters, parameterExtractors);
    Map<String, String> map = testOAuthConfig.getCustomBodyParameters();

    assertThat(map, is(notNullValue()));
    assertThat(map.size(), is(0));
  }

  @Test
  public void testGetCustomHeaders() {
    Optional<OAuthObjectStoreConfig> mockStoreConfig = Optional.of(mock(OAuthObjectStoreConfig.class));
    CustomOAuthParameters parameters = new CustomOAuthParameters();
    Map<Field, String> parameterExtractors = new HashMap<>();

    testOAuthConfig = new TestOAuthConfig("", mockStoreConfig, parameters, parameterExtractors);
    Map<String, String> map = testOAuthConfig.getCustomHeaders();

    assertThat(map, is(notNullValue()));
    assertThat(map.size(), is(0));
  }

  @Test
  public void testGetParameterExtractors() {
    Optional<OAuthObjectStoreConfig> mockStoreConfig = Optional.of(mock(OAuthObjectStoreConfig.class));
    CustomOAuthParameters parameters = new CustomOAuthParameters();
    Map<Field, String> parameterExtractors = new HashMap<>();

    testOAuthConfig = new TestOAuthConfig("", mockStoreConfig, parameters, parameterExtractors);
    Map<String, String> map = testOAuthConfig.getParameterExtractors();

    assertThat(map, is(parameterExtractors));
  }

  private static class TestOAuthConfig extends OAuthConfig {

    public TestOAuthConfig(String ownerConfigName, Optional optional, CustomOAuthParameters customOAuthParameters,
                           Map parameterExtractors) {
      super(ownerConfigName, optional, customOAuthParameters, parameterExtractors);
    }

    @Override
    public OAuthGrantType getGrantType() {
      return null;
    }
  }
}
