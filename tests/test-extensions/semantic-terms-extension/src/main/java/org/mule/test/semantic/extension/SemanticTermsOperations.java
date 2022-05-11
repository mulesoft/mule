/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
