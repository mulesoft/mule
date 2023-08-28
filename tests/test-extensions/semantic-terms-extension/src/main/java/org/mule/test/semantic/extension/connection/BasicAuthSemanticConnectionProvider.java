/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.semantic.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.BasicAuth;
import org.mule.sdk.api.annotation.semantics.security.Password;
import org.mule.sdk.api.annotation.semantics.security.Username;

@BasicAuth
@Alias("basic-auth")
public class BasicAuthSemanticConnectionProvider extends SemanticTermsConnectionProvider {

  @Parameter
  @Username
  private String username;

  @Parameter
  @Password
  private String password;

}
