/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.semantic.extension;

import static org.mule.sdk.api.annotation.param.MediaType.APPLICATION_JSON;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.connectivity.UrlPath;
import org.mule.sdk.api.annotation.semantics.security.AccountId;
import org.mule.sdk.api.annotation.semantics.security.TenantIdentifier;

public class SemanticTermsOperations {

  @MediaType(APPLICATION_JSON)
  public String send(@Host String host, @Password String password, @AccountId String accountId) {
    return "";
  }

  @MediaType(APPLICATION_JSON)
  public String redirect(@UrlPath String path) {
    return "";
  }

  @MediaType(APPLICATION_JSON)
  public String bill(@TenantIdentifier String tenant, @AccountId String account) {
    return "";
  }
}
