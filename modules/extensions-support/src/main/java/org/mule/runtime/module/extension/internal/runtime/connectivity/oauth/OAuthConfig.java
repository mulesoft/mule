/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

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
  private final Map<String, String> customParameters;
  private final Map<Field, String> parameterExtractors;

  public OAuthConfig(String ownerConfigName,
                     Optional<OAuthObjectStoreConfig> storeConfig,
                     Map<String, String> customParameters,
                     Map<Field, String> parameterExtractors) {
    this.ownerConfigName = ownerConfigName;
    this.storeConfig = storeConfig;
    this.customParameters = customParameters;
    this.parameterExtractors = parameterExtractors;
  }

  public abstract T getGrantType();

  public String getOwnerConfigName() {
    return ownerConfigName;
  }

  public Optional<OAuthObjectStoreConfig> getStoreConfig() {
    return storeConfig;
  }

  public Map<String, String> getCustomParameters() {
    return customParameters;
  }

  public Map<Field, String> getParameterExtractors() {
    return parameterExtractors;
  }
}
