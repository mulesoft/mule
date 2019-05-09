/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

public class ClientCredentialsConfig extends OAuthConfig<ClientCredentialsGrantType> {

  private final String tokenUrl;
  private final CredentialsPlacement credentialsPlacement;
  private final ClientCredentialsGrantType grantType;

  public ClientCredentialsConfig(String ownerConfigName,
                                 Optional<OAuthObjectStoreConfig> storeConfig,
                                 Map<String, String> customParameters,
                                 Map<Field, String> parameterExtractors,
                                 String tokenUrl,
                                 CredentialsPlacement credentialsPlacement,
                                 ClientCredentialsGrantType grantType) {
    super(ownerConfigName, storeConfig, customParameters, parameterExtractors);
    this.tokenUrl = tokenUrl;
    this.credentialsPlacement = credentialsPlacement;
    this.grantType = grantType;
  }

  public String getTokenUrl() {
    return tokenUrl;
  }

  public CredentialsPlacement getCredentialsPlacement() {
    return credentialsPlacement;
  }

  @Override
  public ClientCredentialsGrantType getGrantType() {
    return grantType;
  }
}
