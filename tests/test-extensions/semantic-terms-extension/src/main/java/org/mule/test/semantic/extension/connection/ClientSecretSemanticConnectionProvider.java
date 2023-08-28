/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.semantic.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.ApiKeyAuth;
import org.mule.sdk.api.annotation.semantics.security.ClientId;
import org.mule.sdk.api.annotation.semantics.security.ClientSecret;

@ApiKeyAuth
@Alias("client-secret")
public class ClientSecretSemanticConnectionProvider extends SemanticTermsConnectionProvider {

  @Parameter
  @ClientId
  private String clientId;

  @Parameter
  @ClientSecret
  private String clientSecret;

}
