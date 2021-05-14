/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.semantic.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;
import org.mule.sdk.api.annotation.semantics.security.AccountId;
import org.mule.sdk.api.annotation.semantics.security.ConnectionId;
import org.mule.sdk.api.annotation.semantics.security.Secret;
import org.mule.sdk.api.annotation.semantics.security.SecretToken;
import org.mule.sdk.api.annotation.semantics.security.SecurityToken;
import org.mule.sdk.api.annotation.semantics.security.SessionId;
import org.mule.sdk.api.annotation.semantics.security.TenantIdentifier;
import org.mule.sdk.api.annotation.semantics.security.TokenId;

@Alias("custom")
public class CustomAuthSemanticConnectionProvider extends SemanticTermsConnectionProvider {

  @Parameter
  @ConnectionId
  private String connectionId;

  @Parameter
  @TenantIdentifier
  private String tenantName;

  @Parameter
  @TokenId
  private String tokenId;

  @Parameter
  @SecurityToken
  private String securityToken;

  @Parameter
  @SecretToken
  private String secretToken;

  @Parameter
  @SessionId
  private String sessionId;

  @Parameter
  @AccountId
  private String accountId;

  @Parameter
  @Secret
  private byte[] certificate;

  @Parameter
  @Secret
  @ExcludeFromConnectivitySchema
  private String secretNumber;

}
