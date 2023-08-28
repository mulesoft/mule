/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.semantic.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.ApiKeyAuth;
import org.mule.sdk.api.annotation.semantics.security.ApiKey;

@ApiKeyAuth
@Alias("api-key")
public class ApiKeySemanticConnectionProvider extends SemanticTermsConnectionProvider {

  @Parameter
  @ApiKey
  private String apiKey;

}
