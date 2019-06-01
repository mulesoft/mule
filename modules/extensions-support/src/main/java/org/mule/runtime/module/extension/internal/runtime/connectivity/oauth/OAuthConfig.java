/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * Groups the sum of all the parameters that a user configured in order to consume
 * an OAuth provider through an extension
 *
 * @since 4.0
 */
public abstract class OAuthConfig<T extends OAuthGrantType> {

  private final String ownerConfigName;
  private final Optional<OAuthObjectStoreConfig> storeConfig;
  private final MultiMap<String, String> customParameters;
  private final MultiMap<String, String> customHeaders;
  private final Map<Field, String> parameterExtractors;

  public OAuthConfig(String ownerConfigName,
                     Optional<OAuthObjectStoreConfig> storeConfig,
                     MultiMap<String, String> customParameters,
                     MultiMap<String, String> customHeaders,
                     Map<Field, String> parameterExtractors) {
    this.ownerConfigName = ownerConfigName;
    this.storeConfig = storeConfig;
    this.customParameters = customParameters;
    this.customHeaders = customHeaders;
    this.parameterExtractors = parameterExtractors;
  }

  public abstract T getGrantType();

  public String getOwnerConfigName() {
    return ownerConfigName;
  }

  public Optional<OAuthObjectStoreConfig> getStoreConfig() {
    return storeConfig;
  }

  public MultiMap<String, String> getCustomParameters() {
    return customParameters;
  }

  public MultiMap<String, String> getCustomHeaders() {
    return customHeaders;
  }

  public Map<Field, String> getParameterExtractors() {
    return parameterExtractors;
  }
}
