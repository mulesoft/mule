/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * Groups the sum of all the parameters that a user configured in order to consume an OAuth provider through an extension
 *
 * @since 4.0
 */
public abstract class OAuthConfig<T extends OAuthGrantType> {

  private final String ownerConfigName;
  private final Optional<OAuthObjectStoreConfig> storeConfig;
  private final CustomOAuthParameters customOAuthParameters;
  private final Map<Field, String> parameterExtractors;

  public OAuthConfig(String ownerConfigName,
                     Optional<OAuthObjectStoreConfig> storeConfig,
                     CustomOAuthParameters customOAuthParameters,
                     Map<Field, String> parameterExtractors) {
    this.ownerConfigName = ownerConfigName;
    this.storeConfig = storeConfig;
    this.customOAuthParameters = customOAuthParameters;
    this.parameterExtractors = parameterExtractors;
  }

  public abstract T getGrantType();

  public String getOwnerConfigName() {
    return ownerConfigName;
  }

  public Optional<OAuthObjectStoreConfig> getStoreConfig() {
    return storeConfig;
  }

  public MultiMap<String, String> getCustomQueryParameters() {
    return customOAuthParameters.getQueryParams();
  }

  /**
   * @since 4.5.0
   */
  public Map<String, String> getCustomBodyParameters() {
    return customOAuthParameters.getBodyParams();
  }

  public MultiMap<String, String> getCustomHeaders() {
    return customOAuthParameters.getHeaders();
  }

  public Map<Field, String> getParameterExtractors() {
    return parameterExtractors;
  }
}
